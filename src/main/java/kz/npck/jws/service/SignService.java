package kz.npck.jws.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.jca.JCAContext;
import kz.gamma.jce.provider.GammaTechProvider;
import kz.npck.jws.config.SignProperties;
import kz.npck.jws.model.SignResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;

@Service
public class SignService {

    private static final Logger log = LoggerFactory.getLogger(SignService.class);

    /** Как в Example.kt: GAMMA принимает OID из KZObjectIndentifiers.GOSTR_34_10_2015_SIGNATURE_512, не строку "GOST3410-2015-512". */
    private static final String[] SIGNATURE_ALGORITHM_CANDIDATES = {
            "GOST3410-2015-512",
            gammaOidGost2015_512(),  // OID из kz.gamma.asn1.cryptopro.KZObjectIndentifiers (как в Example.kt)
            "GOST3411withGOST3410-2012-512",
            "GOST3410-2012-512",
            "GOST3411withGOST3410-2012-256",
            "GOST3410-2012-256",
            "1.2.398.3.10.1.1.2.1",  // OID ГОСТ Р 34.10-2012 512
            "1.2.398.3.10.1.1.1.1",  // OID ГОСТ Р 34.10-2012 256
    };

    private static String gammaOidGost2015_512() {
        try {
            Class<?> c = Class.forName("kz.gamma.asn1.cryptopro.KZObjectIndentifiers");
            Object oid = c.getField("GOSTR_34_10_2015_SIGNATURE_512").get(null);
            return oid != null ? oid.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private final PrivateKey privateKey;
    private final X509Certificate certificate;
    private final SignProperties props;
    private final String resolvedSignatureAlgorithm;
    private final String resolvedProvider;

    public SignService(SignProperties props) throws Exception {
        this.props = props;

        Security.addProvider(new GammaTechProvider());

        // Загружаем PFX стандартным JDK — у GammaTechProvider баг в engineLoad (replaceAll по бинарным данным).
        // GAMMA используем только для подписи (Signature), не для KeyStore.
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(props.getKeystorePath())) {
            store.load(fis, props.getKeystorePassword().toCharArray());
        }

        if (!store.aliases().hasMoreElements()) {
            throw new IllegalStateException("Keystore has no entries (wrong password or empty PFX?)");
        }
        String alias = store.aliases().nextElement();
        privateKey = (PrivateKey) store.getKey(alias, props.getKeystorePassword().toCharArray());
        certificate = (X509Certificate) store.getCertificate(alias);

        var resolved = resolveSignatureAlgorithm(privateKey, props.getProvider(), props.getSignatureAlgorithm());
        this.resolvedSignatureAlgorithm = resolved.algorithm;
        this.resolvedProvider = resolved.provider;
        log.info("GOST signature: algorithm={}, provider={}", resolvedSignatureAlgorithm, resolvedProvider != null ? resolvedProvider : "default");
    }

    private static final class ResolvedAlg {
        final String algorithm;
        final String provider;

        ResolvedAlg(String algorithm, String provider) {
            this.algorithm = algorithm;
            this.provider = provider;
        }
    }

    /**
     * GAMMA/Tumar могут регистрировать ГОСТ под разными именами. Выбираем первый рабочий вариант.
     * Пробуем с указанным провайдером, затем без провайдера (поиск по всем зарегистрированным).
     */
    private static ResolvedAlg resolveSignatureAlgorithm(PrivateKey key, String provider, String configured) {
        if (configured == null || configured.isEmpty()) {
            configured = "GOST3410-2015-512";
        }
        String[] candidates = new String[SIGNATURE_ALGORITHM_CANDIDATES.length + 1];
        candidates[0] = configured;
        System.arraycopy(SIGNATURE_ALGORITHM_CANDIDATES, 0, candidates, 1, SIGNATURE_ALGORITHM_CANDIDATES.length);
        for (String alg : candidates) {
            if (alg == null || alg.isEmpty()) continue;
            for (String prov : new String[]{provider, null}) {
                try {
                    Signature sgn = prov != null ? Signature.getInstance(alg, prov) : Signature.getInstance(alg);
                    sgn.initSign(key);
                    return new ResolvedAlg(alg, prov);
                } catch (GeneralSecurityException ignored) {
                    // пробуем следующий вариант
                }
            }
        }
        throw new IllegalStateException(
                "No GOST signature algorithm found for provider " + provider + ". Tried: " + String.join(", ", candidates));
    }

    public SignResponse signText(String text) throws Exception {

        byte[] certBytes = certificate.getEncoded();

        MessageDigest sha = MessageDigest.getInstance("SHA-256");

        String x5tS256 = Base64.getUrlEncoder().encodeToString(sha.digest(certBytes));

        String dn = certificate.getSubjectX500Principal().getName();

        JWSHeader header = new JWSHeader.Builder(new JWSAlgorithm(props.getJwsAlgorithm()))
                .keyID(dn)
                .x509CertSHA256Thumbprint(new Base64URL(x5tS256))
                .build();

        JWSObject jws = new JWSObject(header, new Payload(text));
        jws.sign(new GostJwsSigner(privateKey, resolvedSignatureAlgorithm, resolvedProvider, props.getJwsAlgorithm()));

        return new SignResponse(jws.serialize(), dn, x5tS256);
    }

    private static final class GostJwsSigner implements JWSSigner {
        private final PrivateKey privateKey;
        private final String signatureAlgorithm;
        private final String provider;
        private final Set<JWSAlgorithm> supported;

        private GostJwsSigner(PrivateKey privateKey, String signatureAlgorithm, String providerOrNull, String jwsAlg) {
            this.privateKey = privateKey;
            this.signatureAlgorithm = signatureAlgorithm;
            this.provider = providerOrNull;
            this.supported = Collections.singleton(new JWSAlgorithm(jwsAlg));
        }

        @Override
        public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
            try {
                Signature sgn = provider != null ? Signature.getInstance(signatureAlgorithm, provider) : Signature.getInstance(signatureAlgorithm);
                sgn.initSign(privateKey);
                sgn.update(signingInput);
                return Base64URL.encode(sgn.sign());
            } catch (GeneralSecurityException e) {
                throw new JOSEException("GOST JWS signing failed", e);
            }
        }

        @Override
        public Set<JWSAlgorithm> supportedJWSAlgorithms() {
            return supported;
        }

        @Override
        public JCAContext getJCAContext() {
            return new JCAContext();
        }
    }

}