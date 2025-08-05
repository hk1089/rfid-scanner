package com.app.drinker

import BreathalyzerSDK.API.BACtrackAPI
import BreathalyzerSDK.API.BACtrackAPICallbacks
import BreathalyzerSDK.Constants.BACTrackDeviceType
import BreathalyzerSDK.Constants.BACtrackUnit
import BreathalyzerSDK.Constants.Errors
import BreathalyzerSDK.Exceptions.BluetoothLENotSupportedException
import BreathalyzerSDK.Exceptions.BluetoothNotEnabledException
import BreathalyzerSDK.Exceptions.LocationServicesNotEnabledException
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class AnalyzerClass {
    private var mAPI: BACtrackAPI? = null
    private lateinit var context: Context
    private var requiresUseCount = false
    var onStatusUpdate: ((String) -> Unit)? = null // Callback for status updates

    fun startSDK(cnx: Context) {
        val apiKey = "caea1ef90e38464eac98c5d6d37439"
        context = cnx
        try {
            mAPI = BACtrackAPI(context, object : BACtrackAPICallbacks{
                override fun BACtrackAPIKeyDeclined(p0: String?) {
                    Log.d("TAG", "BACtrackAPIKeyDeclined: " + p0)
                    setStatus("API Key Declined")
                }

                override fun BACtrackAPIKeyAuthorized() {
                    setStatus("BACtrackAPIKeyAuthorized")
                    Log.d("TAG", "BACtrackAPIKeyAuthorized: ")
                }

                override fun BACtrackConnected(bacTrackDeviceType: BACTrackDeviceType) {
                        val name: String = bacTrackDeviceType.getDisplayName()
                        setStatus("${context.getString(R.string.TEXT_CONNECTED)} to ${name}")

                    Log.d("TAG", "BACtrackConnected: " + bacTrackDeviceType.name)
                }

                override fun BACtrackDidConnect(p0: String?) {
                    setStatus(context.getString(R.string.TEXT_DISCOVERING_SERVICES))
                }

                override fun BACtrackDisconnected() {
                    setStatus(context.getString(R.string.TEXT_DISCONNECTED))
                }

                override fun BACtrackConnectionTimeout() {
                    Log.d("TAG", "BACtrackConnectionTimeout: " )
                }

                override fun BACtrackFoundBreathalyzer(p0: BACtrackAPI.BACtrackDevice?) {
                    setStatus("Device found nearby")
                    Log.d("TAG", "BACtrackFoundBreathalyzer: " + p0?.rssi)
                }

                override fun BACtrackCountdown(currentCountdownCount: Int) {
                    setStatus(context.getString(R.string.TEXT_COUNTDOWN) + " " + currentCountdownCount)
                    Log.d("TAG", "BACtrackCountdown: " + currentCountdownCount)
                }

                override fun BACtrackStart() {
                    setStatus(context.getString((R.string.TEXT_BLOW_NOW)))
                    Log.d("TAG", "BACtrackStart: ")
                }

                override fun BACtrackBlow(v: Float) {
                    val text = "Keep blowing " + v
                    setStatus(text)
                    Log.d("TAG", "BACtrackBlow: " + v)
                }

                override fun BACtrackAnalyzing() {
                    setStatus(context.getString(R.string.TEXT_ANALYZING))
                    Log.d("TAG", "BACtrackAnalyzing: " )
                }

                override fun BACtrackResults(measuredBac: Float) {
                    var text: String = context.getString(R.string.TEXT_FINISHED) + " " + measuredBac
                    //if (requiresUseCount) {
                    //    text = text + "\n\n You can now obtain Use Count"
                    //}
                    setStatus(text)
                    Log.d("TAG", "BACtrackResults: " + measuredBac)
                }

                override fun BACtrackFirmwareVersion(version: String?) {
                    setStatus(context.getString(R.string.TEXT_FIRMWARE_VERSION) + " " + version)
                    Log.d("TAG", "BACtrackFirmwareVersion: " + version)
                }

                override fun BACtrackSerial(serialHex: String?) {
                    setStatus(context.getString(R.string.TEXT_SERIAL_NUMBER) + " " + serialHex)
                    Log.d("TAG", "BACtrackSerial: " + serialHex)
                }

                override fun BACtrackUseCount(useCount: Int) {
                    Log.d("TAG", "BACtrackUseCount: " + useCount)
                    // C6/C8 bug in hardware does not allow getting use count
                    if (useCount == 4096) {
                        setStatus("Cannot retrieve use count for C6/C8 devices")
                    } else if (useCount == -1) {
                        requiresUseCount = true
                        setStatus("You must take a test before obtaining use count\n Tap on Start Test Countdown")
                    } else {
                        setStatus(context.getString(R.string.TEXT_USE_COUNT) + " " + useCount)
                    }
                }

                override fun BACtrackBatteryVoltage(voltage: Float) {
                    Log.d("TAG", "BACtrackBatteryVoltage: " + voltage)
                }

                override fun BACtrackBatteryLevel(level: Int) {
                    Log.d("TAG", "BACtrackBatteryLevel: " + level)
                    val message: String = context.getString(R.string.TEXT_BATTERY_LEVEL) + " " + level
                    setStatus(String.format("\n%s", message))
                }

                override fun BACtrackError(errorCode: Int) {
                    if (errorCode == Errors.ERROR_BLOW_ERROR.toInt()) {
                        setStatus(context.getString(R.string.TEXT_ERR_BLOW_ERROR))
                    }
                    Log.d("TAG", "BACtrackError: " + errorCode)
                }

                override fun BACtrackUnits(p0: BACtrackUnit?) {
                    Log.d("TAG", "BACtrackUnits: " + p0?.name)
                }

            }, apiKey)
        } catch (e: BluetoothLENotSupportedException) {
            e.printStackTrace()
            setStatus(context.getString(R.string.TEXT_ERR_BLE_NOT_SUPPORTED))
        } catch (e: BluetoothNotEnabledException) {
            e.printStackTrace()
            setStatus(context.getString(R.string.TEXT_ERR_BT_NOT_ENABLED))
        } catch (e: LocationServicesNotEnabledException) {
            e.printStackTrace()
            setStatus(context.getString(R.string.TEXT_ERR_LOCATIONS_NOT_ENABLED))
        }
    }

    private fun setStatus(message: String?) {
        Log.d("TAG", "Status: " + message)
        message?.let { onStatusUpdate?.invoke(it) } // Invoke the callback
       // breathalyzerStateTextView.setText(message)
    }

    fun requestAllPermissions(activity: Activity, requestCode: Int = 100): Boolean {
        val permissions = arrayOf(
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        return if (permissionsToRequest.isEmpty()) {
            true
        } else {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), requestCode)
            false
        }
    }

    fun connectNearestClicked() {
        if (mAPI != null) {
            setStatus(context.getString(R.string.TEXT_CONNECTING))
            mAPI!!.connectToNearestBreathalyzer()

        }
    }
    fun isConnected() = mAPI?.isConnected ?: false
    fun getUseCount() {
        if (mAPI != null) {
            val count = mAPI!!.getUseCount()
            Log.d("TAG", "getUseCount: count " + count)
        }
    }

    fun startBlowProcessClicked() {
        if (mAPI != null) {
            mAPI!!.startCountdown()
        }
    }

    fun requestBatteryLevelClicked() {
        if (mAPI != null) {
            mAPI!!.getBreathalyzerBatteryVoltage()
        }
    }
    fun disconnectClicked() {
        if (mAPI != null) {
            mAPI!!.disconnect()
        }
    }
    fun getSerialNumberClicked() {
        if (mAPI != null) {
            mAPI!!.getSerialNumber()
        }
    }
}
