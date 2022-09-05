package com.weatherxm.ui.notification

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.ui.home.HomeActivity

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If this activity is the root activity of the task, the app is not running
        if (isTaskRoot) {
            // Start the app before finishing
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            intent.extras?.let {
                // copy all extras
                intent.putExtras(it)
            }
            startActivity(intent)
        }

        // Now finish, which will drop you to the activity at which you were at the top of the task stack
        finish()
    }
}
