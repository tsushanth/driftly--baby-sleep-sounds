package com.factory.driftlybabysleepsounds

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.factory.driftlybabysleepsounds.billing.BillingManager
import com.factory.driftlybabysleepsounds.data.local.AppDatabase
import com.factory.driftlybabysleepsounds.data.local.ThemePreferences
import com.factory.driftlybabysleepsounds.data.repository.SoundRepository
import com.factory.driftlybabysleepsounds.premium.PremiumManager

open class DriftlyApplication : Application() {

    open val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    open val repository: SoundRepository by lazy { SoundRepository(database.favoriteDao(), database.mixDao()) }
    open val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
    open val premiumManager: PremiumManager by lazy { PremiumManager(this) }
    open val billingManager: BillingManager by lazy {
        BillingManager(
            this,
            onVerifiedPurchases = { verifiedPurchases -> premiumManager.applyVerifiedPurchases(verifiedPurchases) }
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        billingManager.startConnection()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "driftly_playback_channel"
    }
}
