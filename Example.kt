package kz.npck.jws

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nimbusds.jose.*
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.util.Base64URL
import kz.gamma.asn1.cryptopro.KZObjectIndentifiers
import kz.gamma.jce.provider.GammaTechProvider
import kz.gamma.jce.provider.GammaTechProvider.PROVIDER_NAME
import kz.gamma.tumarcsp.params.StoreObjectParam
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Security.addProvider
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*


fun main() {
    val path = "/path/to/2015.pfx"
    val password = "password"

    addProvider(GammaTechProvider())

    val store = KeyStore.getInstance("PKCS12", PROVIDER_NAME).apply {
        load(ByteArrayInputStream(path.toByteArray()), password.toCharArray())
    }

    val prm = store.aliases().nextElement() as StoreObjectParam

    val publicKey = store.getCertificate(prm.getSn())
    val privateKey = store.getKey(prm.getSn(), password.toCharArray()) as PrivateKey

    val x509cert = publicKey.encoded
        .inputStream()
        .use { certIS ->
            val x509 = CertificateFactory.getInstance("X.509")
                .generateCertificate(certIS) as X509Certificate
            x509.checkValidity()
            x509
        }

    val x5t = getX5tSha256(publicKey.encoded)
    val dn = x509cert.subjectX500Principal.name
    println(dn)
    println(x5t)

    //Указать БИН и ИИН
    val payload = PayloadModel("<BIN>","<IIN>", ConsentType.BIOMETRY)
    val mapper = jacksonObjectMapper().registerKotlinModule()
    val jsonString = mapper.writeValueAsString(payload)

    println(jsonString)

    val alg = JWSAlgorithm("GOST3410-2015-512")
    val header = JWSHeader.Builder(alg)
        .keyID(dn)
        .x509CertSHA256Thumbprint(Base64URL(x5t))
        .build()

    println(header.toString())

    val jwsObject = JWSObject(header, Payload(jsonString))

    jwsObject.sign(GostSigner(privateKey))

    //Формирование заголовка, переменная s должна быть передана в заголовке
    val s = jwsObject.serialize()
    println(s)
}

fun getX5tSha256(encoded: ByteArray): String {
    val sha1 = MessageDigest.getInstance("SHA-256")

    return Base64.getUrlEncoder().encodeToString(sha1.digest(encoded))
}

class PayloadModel (
    val bin: String,
    val iin: String,
    val consentType: ConsentType
)

enum class ConsentType(val value: kotlin.String) {

    BIOMETRY("BIOMETRY"),
    DIGITAL_SIGNATURES("DIGITAL_SIGNATURES"),
    OTP("OTP"),
    DIGITAL_ID("DIGITAL_ID"),
    PAPER("PAPER");
}

class GostSigner(val privateKey: PrivateKey) : JWSSigner {

    override fun sign(
        header: JWSHeader?,
        signingInput: ByteArray?
    ): Base64URL? {
        val sgn = Signature.getInstance(KZObjectIndentifiers.GOSTR_34_10_2015_SIGNATURE_512.toString(), "GAMMA")
        sgn.initSign(privateKey)
        sgn.update(signingInput)
        val sign = sgn.sign()

        return Base64URL.encode(sign)
    }

    override fun supportedJWSAlgorithms(): Set<JWSAlgorithm?>? {
        val algs: MutableSet<JWSAlgorithm?> = LinkedHashSet<JWSAlgorithm?>()
        algs.add(JWSAlgorithm("GOST3410-2015-512"))
        return algs;
    }

    override fun getJCAContext(): JCAContext? {
        return null;
    }
}