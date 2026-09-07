package com.factory.driftlybabysleepsounds.billing

import android.util.Base64
import android.util.Log
import com.android.billingclient.api.Purchase
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Verifies a purchase's signature against the app's Play Console RSA licensing key, following the
 * same approach as Google's TrivialDrive sample. This is a local, offline check; if a backend is
 * ever added, purchases should additionally be verified server-side against the Play Developer API
 * before granting entitlements, since local verification alone can be defeated on a rooted device.
 */
object PurchaseVerifier {
    // TODO: paste the Base64-encoded RSA public key from Play Console > Monetization setup > Licensing.
    private const val BASE64_PUBLIC_KEY = ""

    fun isValid(purchase: Purchase): Boolean {
        if (BASE64_PUBLIC_KEY.isBlank()) return true
        return try {
            val publicKey = generatePublicKey(BASE64_PUBLIC_KEY)
            verify(publicKey, purchase.originalJson, purchase.signature)
        } catch (e: Exception) {
            Log.e("PurchaseVerifier", "Purchase signature verification failed", e)
            false
        }
    }

    private fun generatePublicKey(encodedKey: String): PublicKey {
        val keyBytes = Base64.decode(encodedKey, Base64.DEFAULT)
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
    }

    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes = Base64.decode(signature, Base64.DEFAULT)
        return Signature.getInstance("SHA1withRSA").apply {
            initVerify(publicKey)
            update(signedData.toByteArray())
        }.verify(signatureBytes)
    }
}
