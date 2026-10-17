package com.example.engine

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.receiver.NovaDeviceAdminReceiver

class DeviceAdminHelper(private val context: Context) {
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, NovaDeviceAdminReceiver::class.java)

    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun lockScreen(): Boolean {
        return try {
            if (isDeviceAdminActive()) {
                devicePolicyManager.lockNow()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getActivateAdminIntent(): Intent {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Enable Nova AI Device Admin so the assistant can lock the screen when you say 'Lock my phone'."
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }
}
