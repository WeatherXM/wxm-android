package com.weatherxm.service

import android.app.Activity
import com.weatherxm.ui.notification.NotificationActivity
import no.nordicsemi.android.dfu.BuildConfig
import no.nordicsemi.android.dfu.DfuBaseService

class DfuService : DfuBaseService() {
    // TODO: Use appropriate notification target class
    override fun getNotificationTarget(): Class<out Activity> {
        /*
         * As a target activity the NotificationActivity is returned, not the MainActivity. This is because
         * the notification must create a new task:
         *
         * intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         *
         * when you press it. You can use NotificationActivity to check whether the new activity
         * is a root activity (that means no other activity was open earlier) or that some
         * other activity is already open. In the latter case the NotificationActivity will just be
         * closed. The system will restore the previous activity. However, if the application has been
         * closed during upload and you click the notification, a NotificationActivity will
         * be launched as a root activity. It will create and start the main activity and
         * terminate itself.
         *
         * This method may be used to restore the target activity in case the application
         * was closed or is open. It may also be used to recreate an activity history using
         * startActivities(...).
         */
        return NotificationActivity::class.java
    }

    // TODO: Is this OK for printing progress or shall we return true?
    override fun isDebug(): Boolean {
        return BuildConfig.DEBUG
    }
}
