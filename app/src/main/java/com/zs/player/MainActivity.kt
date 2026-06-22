package com.zs.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.zs.domain.analytics.Analytics

class MainActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.initialize(this)
        val analytics = Analytics.getInstance()
        analytics.logEvent(Analytics.EVENT_SCREEN_VIEW, Bundle().apply {
            putString(Analytics.PARAM_SCREEN_NAME, "MainActivity")
        })
    }
}
