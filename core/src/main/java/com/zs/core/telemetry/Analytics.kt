/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 09-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.core.telemetry

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics

class Analytics {

    // Tag used for logging.
    private val TAG = "AnalyticsImpl"

    private val crashlytics: FirebaseCrashlytics
    private val analytics: FirebaseAnalytics

    private constructor(context: Context) {
        FirebaseApp.initializeApp(context)
        Log.d(TAG, "init")
        analytics = Firebase.analytics
        crashlytics = Firebase.crashlytics.also {
            it.isCrashlyticsCollectionEnabled = true
        }
    }

    fun record(throwable: Throwable) {
        // Record the exception using Crashlytics.
        crashlytics.recordException(throwable)
        // Log the exception message.
        Log.d(TAG, "record: ${throwable.message}")
    }

    fun logEvent(name: String, params: Bundle) {
        analytics.logEvent(name, params)
        // Firebase Analytics automatically logs the event.
    }

    companion object {
        const val EVENT_SCREEN_VIEW: String = "screen_view"
        const val PARAM_SCREEN_NAME: String = "screen_name"

        @Volatile
        private var instance: Analytics? = null

        operator fun invoke(context: Context): Analytics =
            instance ?: synchronized(this) {
                instance ?: Analytics(context).also { instance = it }
            }
    }
}