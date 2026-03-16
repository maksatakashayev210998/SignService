package kz.npck.jws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sign")
public class SignProperties {

    /**
     * Path inside container/host to PKCS#12 file (.pfx/.p12).
     */
    private String keystorePath = "/certs/cert.pfx";

    private String keystorePassword = "password";

    /**
     * Usually "PKCS12".
     */
    private String keystoreType = "PKCS12";

    /**
     * Provider for KeyStore/Signature, usually "GAMMA".
     */
    private String provider = "GAMMA";

    /**
     * JWS alg header, must match verifier expectations.
     */
    private String jwsAlgorithm = "GOST3410-2015-512";

    /**
     * JCA signature algorithm name or OID supported by provider.
     */
    private String signatureAlgorithm = "GOST3410-2015-512";

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    public void setJwsAlgorithm(String jwsAlgorithm) {
        this.jwsAlgorithm = jwsAlgorithm;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }
}

