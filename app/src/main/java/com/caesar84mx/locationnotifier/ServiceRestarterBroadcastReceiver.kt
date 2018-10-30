package com.caesar84mx.locationnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.caesar84mx.locationnotifier.Utility.Companion.APP_TAG
import com.google.android.gms.maps.model.LatLng

class ServiceRestarterBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(APP_TAG, "Restarting service")
        context?.startService(copyExtras(intent, Intent(context, LocationTrackingService::class.java)))
    }

    private fun copyExtras(from: Intent?, to: Intent): Intent {
        val targetLocation = from?.getParcelableExtra<LatLng>(Utility.TARGET_LOCATION_KEY)
        val radius = from?.getDoubleExtra(Utility.TARGET_RADIUS_KEY, 100.0)
        val phoneNumber = from?.getStringExtra(Utility.TARGET_PHONE_NUMBER_KEY)
        val notificationMessage = from?.getStringExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY) ?: ""

        to
            .putExtra(Utility.TARGET_LOCATION_KEY, targetLocation)
            .putExtra(Utility.TARGET_RADIUS_KEY, radius)
            .putExtra(Utility.TARGET_PHONE_NUMBER_KEY, phoneNumber)
            .putExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY, notificationMessage)

        return to
    }
}