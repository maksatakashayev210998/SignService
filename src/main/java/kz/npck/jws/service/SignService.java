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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

@Service
public class SignService {

    private static final Logger log = LoggerFactory.getLogger(SignService.class);

    /** Как в Example.kt: GAMMA принимает OID из KZObjectIndentifiers.GOSTR_34_10_2015_SIGNATURE_512, не строку "GOST3410-2015-512". */
    private static final String[] SIGNATURE_ALGORITHM_CANDIDATES = {
            "GOST3410-2015-512",
            gammaOidGost2015_512(),  // OID из kz.gamma.asn1.cryptopro.KZObjectIndentifiers (как в Example.kt)
            // "GOST3411withGOST3410-2012-512",
            // "GOST3410-2012-512",
            // "GOST3411withGOST3410-2012-256",
            // "GOST3410-2012-256",
            // "1.2.398.3.10.1.1.2.1",  // OID ГОСТ Р 34.10-2012 512
            // "1.2.398.3.10.1.1.1.1",  // OID ГОСТ Р 34.10-2012 256
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

        String path = props.getKeystorePath();
        java.nio.file.Path pathObj = Paths.get(path);
        if (!Files.exists(pathObj)) {
            throw new IllegalStateException("Keystore file not found: " + path + " (check volume mount, e.g. ./certs:/certs)");
        }
        byte[] keystoreBytes = Files.readAllBytes(pathObj);
        char[] password = props.getKeystorePassword().toCharArray();
        if (password == null || password.length == 0) {
            throw new IllegalStateException("Keystore password is empty (set SIGN_KEYSTORE_PASSWORD env)");
        }
        log.info("Loading keystore from {} ({} bytes), password length={}", path, keystoreBytes.length, password.length);

        // Сначала пробуем загрузить через GAMMA — тогда ключ будет "известен" провайдеру и подпись не даст "Unknown private key".
        // Как в Example.kt: KeyStore.getInstance("PKCS12", "GAMMA"), alias может быть StoreObjectParam с getSn().
        KeyStore store = loadKeyStore(path, keystoreBytes, password);
        Object keyId = getKeyId(store);
        String alias = keyId == null ? null : (keyId instanceof String ? (String) keyId : keyId.toString());
        PrivateKey pk = (PrivateKey) store.getKey(alias, password);
        X509Certificate cert = (X509Certificate) store.getCertificate(alias);
        if (pk == null || cert == null) {
            var pair = getFirstKeyAndCert(store, password);
            if (pair != null) {
                pk = pair.key;
                cert = pair.cert;
            }
        }
        if (pk == null || cert == null) {
            logErrorDetails(store, alias, password);
            String hint = (cert != null && pk == null)
                    ? "PFX opens and contains a certificate, but NO PRIVATE KEY was found. Re-export the PFX from TumarCSP/token WITH the private key, or the key may use a different password."
                    : "Keystore has no key/cert for this password. Check password and that PFX contains a private key.";
            throw new IllegalStateException(hint + " (alias '" + alias + "' -> key=" + (pk != null) + " cert=" + (cert != null) + ")");
        }
        this.privateKey = pk;
        this.certificate = cert;

        var resolved = resolveSignatureAlgorithm(privateKey, props.getProvider(), props.getSignatureAlgorithm());
        this.resolvedSignatureAlgorithm = resolved.algorithm;
        this.resolvedProvider = resolved.provider;
        log.info("GOST signature: algorithm={}, provider={}", resolvedSignatureAlgorithm, resolvedProvider != null ? resolvedProvider : "default");
    }

    /**
     * Загрузка PFX: сначала через GAMMA (ключ тогда подходит для GAMMA Signature), при ошибке — через JDK.
     */
    private static KeyStore loadKeyStore(String keystorePath, byte[] keystoreBytes, char[] password) throws Exception {
        try {
            KeyStore store = KeyStore.getInstance("PKCS12", "GAMMA");
            byte[] keystorePathBytes = keystorePath.getBytes(StandardCharsets.UTF_8);
            store.load(new ByteArrayInputStream(keystorePathBytes), password);
            if (store.size() > 0) {
                log.info("Keystore loaded with GAMMA provider (path mode)");
                return store;
            }
            log.warn("GAMMA KeyStore path-mode load returned zero entries, trying raw PKCS12 bytes");
        } catch (Exception pathModeException) {
            log.warn("GAMMA KeyStore path-mode load failed ({}), trying raw PKCS12 bytes", pathModeException.getMessage());
        }

        try {
            KeyStore store = KeyStore.getInstance("PKCS12", "GAMMA");
            store.load(new ByteArrayInputStream(keystoreBytes), password);
            if (store.size() > 0) {
                log.info("Keystore loaded with GAMMA provider (raw-bytes mode)");
                return store;
            }
            log.warn("GAMMA KeyStore raw-bytes mode returned zero entries, falling back to JDK PKCS12");
        } catch (Exception rawBytesException) {
            log.warn("GAMMA KeyStore raw-bytes mode failed ({}), falling back to JDK PKCS12", rawBytesException.getMessage());
        }

        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(new ByteArrayInputStream(keystoreBytes), password);
        log.info("JDK PKCS12 loaded, store.size()={}", store.size());
        return store;
    }

    /** Перебор всех алиасов: для JDK после fallback первый alias может не подходить. */
    @SuppressWarnings("unchecked")
    private static KeyCertPair getFirstKeyAndCert(KeyStore store, char[] password) {
        try {
            Enumeration<String> aliases = store.aliases();
            int idx = 0;
            while (aliases.hasMoreElements()) {
                String a;
                Object next = aliases.nextElement();
                a = next != null ? next.toString() : null;
                if (a == null) continue;
                idx++;
                try {
                    PrivateKey k = (PrivateKey) store.getKey(a, password);
                    X509Certificate c = (X509Certificate) store.getCertificate(a);
                    log.info("alias[{}] '{}' -> key={} cert={}", idx, a, k != null, c != null);
                    if (k != null && c != null) return new KeyCertPair(k, c);
                } catch (Exception e) {
                    log.warn("alias '{}': getKey/getCertificate failed: {}", a, e.getMessage());
                }
            }
            if (idx == 0) log.warn("Keystore has zero aliases");
        } catch (Exception e) {
            log.warn("getFirstKeyAndCert failed: {}", e.getMessage());
        }
        return null;
    }

    private static void logErrorDetails(KeyStore store, String firstAlias, char[] password) {
        try {
            java.util.Enumeration<?> aliases = store.aliases();
            int i = 0;
            while (aliases.hasMoreElements()) {
                Object a = aliases.nextElement();
                String aliasStr = a != null ? a.toString() : "null";
                boolean keyNull = true, certNull = true;
                Exception keyEx = null;
                try {
                    Key k = store.getKey(aliasStr, password);
                    Certificate c = store.getCertificate(aliasStr);
                    keyNull = (k == null);
                    certNull = (c == null);
                } catch (Exception e) {
                    keyEx = e;
                }
                log.error("Keystore alias[{}] '{}': getKey={} getCertificate={} {}", i + 1, aliasStr, !keyNull, !certNull, keyEx != null ? keyEx.getMessage() : "");
                i++;
            }
        } catch (Exception e) {
            log.error("Could not list aliases: {}", e.getMessage());
        }
    }

    private static final class KeyCertPair {
        final PrivateKey key;
        final X509Certificate cert;
        KeyCertPair(PrivateKey key, X509Certificate cert) { this.key = key; this.cert = cert; }
    }

    /**
     * В GAMMA alias может быть StoreObjectParam; ключ/серт получают по getSn(), не по самому alias.
     */
    private static Object getKeyId(KeyStore store) throws Exception {
        if (!store.aliases().hasMoreElements()) {
            throw new IllegalStateException("Keystore has no entries");
        }
        Object alias = store.aliases().nextElement();
        if (alias != null && alias.getClass().getName().contains("StoreObjectParam")) {
            try {
                Object sn = alias.getClass().getMethod("getSn").invoke(alias);
                if (sn != null) return sn;
            } catch (Exception ignored) { }
        }
        return alias;
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