package com.caesar84mx.locationnotifier

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.view.ViewGroup
import com.caesar84mx.locationnotifier.Utility.Companion.log
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_main.*
import java.text.MessageFormat

private const val REQUEST_CODE_LOCATION_POINT = 1
private const val REQUEST_CODE_PHONE_NUM = 2

const val MY_PERMISSION_REQUEST_CODE_READ_CONTACTS = 1
const val MY_PERMISSION_REQUEST_CODE_SEND_SMS = 2

private const val LOCATION_KEY = "location"
private const val RADIUS_KEY = "radius"
private const val PHONE_NUM_KEY = "phone_num"
private const val MESSAGE_KEY = "message"
private const val SWITCH_VISIBILITY_KEY = "switch_visibility"

private const val STARTED_STATE_KEY = "started_state"
private const val MAIN_SCREEN_ENABLED_STATE_KEY = "main_screen_enabled_state"

class MainActivity : Activity(), View.OnClickListener {
    private var location: LatLng? = null
    private var radius: Double? = null
    private var phoneNum: String? = null
    private var message: String? = null
    private var isMainScreenEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }

        setContentView(R.layout.activity_main)

        Utility.checkPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION,
            this
        )
        Utility.checkPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION,
            this
        )

        btnChooseLocation.setOnClickListener(this)
        btnAddContact.setOnClickListener(this)
        btnSubmit.setOnClickListener(this)
        btnStartTask.setOnClickListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        log( "Saving state...")

        outState?.putParcelable(LOCATION_KEY, location)
        outState?.putDouble(RADIUS_KEY, if (radius == null) 100.0 else radius!!)
        outState?.putString(PHONE_NUM_KEY, phoneNum)
        outState?.putString(MESSAGE_KEY, message)
        outState?.putInt(SWITCH_VISIBILITY_KEY, btnStartTask.visibility)
        outState?.putBoolean(STARTED_STATE_KEY, btnStartTask.isEnabled)
        outState?.putBoolean(MAIN_SCREEN_ENABLED_STATE_KEY, isMainScreenEnabled)

        log( "Saved.")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        log( "Retrieving results from other activity...")

        if (data == null) {
            log( "No data received.")
            return
        }

        when (requestCode) {
            REQUEST_CODE_LOCATION_POINT -> processLocationPointRequestResult(data)
            REQUEST_CODE_PHONE_NUM -> processPhoneImportRequestResult(data)
        }

        log( "Done.")
    }

    override fun onClick(v: View?) {
        log( "${this.javaClass.simpleName} - Processing click...")

        when (v?.id) {
            btnChooseLocation.id -> handleChooseLocationButtonClick()
            btnAddContact.id -> handleAddContactButtonClick()
            btnSubmit.id -> handleSubmitButtonClick()
            btnStartTask.id -> handleStartTaskButtonClick()
        }
    }

    private fun processPhoneImportRequestResult(data: Intent) {
        log( "Retrieving contact data...")

        val phones = Utility.retrievePhoneNumbersFromUri(data.data, this)
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(R.string.dialog_title_choose_phone)
            .setItems(phones.toTypedArray()) { _, which ->
                phoneNum = phones[which]
                edPhoneNumber.setText(phoneNum)
            }
            .create()
            .show()
    }

    private fun processLocationPointRequestResult(data: Intent) {
        log( "Retrieving location data...")

        location = data.getParcelableExtra(LOCATION_KEY)
        radius = data.getDoubleExtra(RADIUS_KEY, 0.0)

        val text = MessageFormat.format(
            getString(R.string.tv_on_map_coords),
            location?.latitude,
            location?.longitude,
            radius.toString()
        )
        tvCoordinates.text = text
    }

    private fun restoreState(savedInstanceState: Bundle) {
        log( "Restoring state")

        location = savedInstanceState.getParcelable(LOCATION_KEY)
        radius = savedInstanceState.getDouble(RADIUS_KEY)
        phoneNum = savedInstanceState.getString(PHONE_NUM_KEY)
        message = savedInstanceState.getString(MESSAGE_KEY)

        isMainScreenEnabled = savedInstanceState.getBoolean(MAIN_SCREEN_ENABLED_STATE_KEY)

        val visibility = savedInstanceState.getInt(SWITCH_VISIBILITY_KEY)
        val isStarted = savedInstanceState.getBoolean(STARTED_STATE_KEY)

        val text = MessageFormat.format(
            getString(R.string.tv_on_map_coords),
            location?.latitude,
            location?.longitude,
            radius.toString()
        )

        tvCoordinates.text = text

        edPhoneNumber.setText(phoneNum)
        edShortMessage.setText(message)
        btnStartTask.visibility = visibility
        toggleViewsAvailability(llMainLayout, isMainScreenEnabled)
        btnStartTask.isEnabled = isStarted
    }

    private fun toggleViewsAvailability(parent: View, isEnabled: Boolean) {
        if (parent is ViewGroup) {
            for (i in 0 until parent.childCount) {
                toggleViewsAvailability(parent.getChildAt(i), isEnabled)
            }
        }
        parent.isEnabled = isEnabled
    }

    private fun handleSubmitButtonClick() {
        log( "Handling submit button...")

        message = edShortMessage.text.toString()

        if (location == null || radius == null || phoneNum == null) {
            log( "Not all fields are fulfilled!")

            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(R.string.dialog_error_message)
                .setPositiveButton(getString(R.string.ok_alert_button_text)) { dialog, _ ->
                    dialog.cancel()
                }
                .create()
                .show()
        } else {
            log( "Confirmation alert...")

            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_confirm_data)
                .setMessage(
                    MessageFormat.format(
                        getString(R.string.dialog_confirmation_message),
                        location?.latitude,
                        location?.longitude,
                        radius,
                        phoneNum,
                        message
                    )
                )
                .setNegativeButton(getString(R.string.alert_negative_button_text)) { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton(getString(R.string.save_alert_button_text)) { dialog, _ ->
                    btnStartTask.visibility = View.VISIBLE
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    private fun handleAddContactButtonClick() {
        log( "Handling add from contact button...")

        Utility.checkPermission(Manifest.permission.READ_CONTACTS, MY_PERMISSION_REQUEST_CODE_READ_CONTACTS, this)
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PHONE_NUM)
    }

    private fun handleChooseLocationButtonClick() {
        log( "Handling choose location button")

        val newIntent = Intent(applicationContext, ChooseLocationOnMapActivity::class.java)
        startActivityForResult(newIntent, REQUEST_CODE_LOCATION_POINT)
    }

    private fun handleStartTaskButtonClick() {
        log( "Handling task run switch...")

        Utility.checkPermission(Manifest.permission.SEND_SMS, MY_PERMISSION_REQUEST_CODE_SEND_SMS, this)

        btnStartTask.isEnabled = false
        btnStartTask.text = getString(R.string.btn_started_text)

        isMainScreenEnabled = false

        val intent = Intent(this, LocationTrackingService::class.java)
            .putExtra(Utility.TARGET_LOCATION_KEY, location)
            .putExtra(Utility.TARGET_RADIUS_KEY, radius)
            .putExtra(Utility.TARGET_PHONE_NUMBER_KEY, phoneNum)
            .putExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY, message)

        startService(intent)
        finish()
    }
}
