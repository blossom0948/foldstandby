package com.blossom.foldstand.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

/** Receives the PackageInstaller result and opens Android's confirmation UI when required. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = intent.confirmationIntent()
                if (confirmationIntent == null) {
                    Log.e(TAG, "PackageInstaller requested user action without a confirmation intent")
                    return
                }
                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(confirmationIntent)
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "FoldStand update installed successfully")
            }

            else -> {
                Log.w(
                    TAG,
                    "FoldStand update failed: status=$status message=${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}",
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.confirmationIntent(): Intent? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_INTENT)
    }

    companion object {
        private const val TAG = "FoldStandUpdater"
        const val ACTION_INSTALL_COMMIT = "com.blossom.foldstand.action.INSTALL_COMMIT"
        const val EXTRA_SESSION_ID = "com.blossom.foldstand.extra.SESSION_ID"
    }
}
