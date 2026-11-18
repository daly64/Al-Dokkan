package tn.aldokkan.app

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/** Clé de l'appareil admin : créée dans le Keystore, jamais exportable. Le serveur ne connaît que sa partie publique. */
object DeviceKey {
    private const val ALIAS = "aldokkan_admin"

    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun ensure() {
        if (store().containsAlias(ALIAS)) return
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
        }.generateKeyPair()
    }

    private fun publicBytes(): ByteArray {
        ensure()
        return store().getCertificate(ALIAS).publicKey.encoded
    }

    /** La clé publique (X.509) en base64, pour l'enregistrer sur le serveur. */
    fun publicKey(): String = Base64.encodeToString(publicBytes(), Base64.NO_WRAP)

    /** Court identifiant de la clé (comparé à celui du serveur pour savoir si c'est bien cet appareil). */
    fun id(): String = MessageDigest.getInstance("SHA-256").digest(publicBytes()).joinToString("") { "%02x".format(it) }.take(8)

    fun sign(data: ByteArray): String {
        ensure()
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(store().getKey(ALIAS, null) as PrivateKey)
        sig.update(data)
        return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
    }
}
