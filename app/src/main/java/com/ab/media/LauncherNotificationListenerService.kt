package com.ab.media

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * Android NotificationListenerService implementation.
 *
 * PRIVACY NOTICE:
 * This service is used strictly as the Android-mandated conduit for MediaSessionManager
 * to discover active media sessions without requesting privileged platform permissions.
 *
 * It does NOT read, store, index, or expose any notification contents, message bodies,
 * contacts, or text.
 */
class LauncherNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "MediaNotifListener"

        private var instance: LauncherNotificationListenerService? = null
        private var onConnectivityChangeListener: ((Boolean) -> Unit)? = null

        fun getServiceComponent(context: Context): ComponentName {
            return ComponentName(context, LauncherNotificationListenerService::class.java)
        }

        fun isServiceConnected(): Boolean = instance != null

        fun setConnectivityListener(listener: ((Boolean) -> Unit)?) {
            onConnectivityChangeListener = listener
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected - MediaSessionManager access granted.")
        instance = this
        onConnectivityChangeListener?.invoke(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected.")
        instance = null
        onConnectivityChangeListener?.invoke(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        onConnectivityChangeListener?.invoke(false)
    }
}
