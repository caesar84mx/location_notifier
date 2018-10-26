package com.caesar84mx.locationnotifier

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.util.Log

class Utility private constructor() {
    companion object {
        const val TARGET_LOCATION_KEY = "target_location"
        const val TARGET_RADIUS_KEY = "target_radius"
        const val TARGET_PHONE_NUMBER_KEY = "target_phone_number"
        const val TARGET_NOTIFICATION_MESSAGE_KEY = "target_notification_message"
        const val APP_TAG = "loc_notifier"

        @JvmStatic
        fun checkPermission(permission: String, requestCode: Int, context: Context) {
            Log.d(APP_TAG, "Checking permission: $permission")

            if (ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(APP_TAG, "Has no permission granted. Requesting...")

                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(permission),
                    requestCode
                )

                Log.d(APP_TAG, "Done.")
            }

            Log.d(APP_TAG, "Checked.")
        }

        @JvmStatic
        fun retrievePhoneNumbersFromUri(contactUri: Uri?, context: Context?): List<String> {
            Log.d(APP_TAG, "Retrieving phone number...")

            val result = mutableListOf<String>()

            val cursorId = context?.contentResolver?.query(
                contactUri!!,
                arrayOf(ContactsContract.Contacts._ID),
                null, null, null, null
            )

            // Getting contacts ID
            val contactId = if (cursorId!!.moveToFirst()) {
                cursorId.getString(cursorId.getColumnIndex(ContactsContract.Contacts._ID))
            } else {
                "null"
            }

            cursorId.close()

            //Getting cursor ID
            val cursor = context.contentResolver?.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId), null
            )

            //Getting number
            if (cursor!!.moveToFirst()) {
                do {
                    val num = formatPhoneNumber(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)))
                    result.add(num)
                    Log.d(APP_TAG, "Retrieving number: $num")
                } while (cursor.moveToNext())
            }

            cursor.close()
            Log.d(APP_TAG, "Phone number retrieved.")
            return result
        }

        fun networkProviderAvailable(locationManager: LocationManager?): Boolean {
            return locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        private fun formatPhoneNumber(phoneNumber: String?): String {
            return phoneNumber!!
                .replace("(", "")
                .replace(")", "")
                .replace(" ", "")
                .replace("-", "")
        }
    }
}