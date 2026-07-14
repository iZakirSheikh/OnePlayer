/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 20-07-2024.
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

package com.zs.audiofy

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.zs.audiofy.common.AppConfig
import com.zs.audiofy.common.IAP_NO_ADS
import com.zs.audiofy.common.Res
import com.zs.audiofy.common.SystemFacade
import com.zs.audiofy.common.WindowStyle
import com.zs.audiofy.common.action
import com.zs.audiofy.common.domain
import com.zs.audiofy.common.dynamicModuleName
import com.zs.audiofy.common.featuredProducts
import com.zs.audiofy.common.isDynamicFeature
import com.zs.audiofy.common.isFreemium
import com.zs.audiofy.common.isPurchasable
import com.zs.audiofy.common.products
import com.zs.audiofy.common.richDesc
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.library.RouteLibrary
import com.zs.audiofy.settings.Settings
import com.zs.compose.foundation.getText2
import com.zs.compose.foundation.runCatching
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.compose.theme.snackbar.SnackbarHostState
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.BuildConfig
import com.zs.core.analytics.Analytics
import com.zs.core.billing.Paymaster
import com.zs.core.billing.Product
import com.zs.core.billing.Purchase
import com.zs.core.billing.purchased
import com.zs.core.common.showPlatformToast
import com.zs.core.market.AppMarketManager
import com.zs.core.playback.Remote
import com.zs.preferences.Key
import com.zs.preferences.Key.Key1
import com.zs.preferences.Key.Key2
import com.zs.preferences.Preferences
import com.zs.preferences.intPreferenceKey
import com.zs.preferences.longPreferenceKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen as initSplashScreen
import androidx.navigation.NavController.OnDestinationChangedListener as NavDestListener

private const val TAG = "MainActivity"

// In-app update and review settings
// Maximum staleness days allowed for a flexible update.
// If the app is older than this, an immediate update will be enforced.
private const val FLEXIBLE_UPDATE_MAX_STALENESS_DAYS = 2

// Minimum number of app launches before prompting for a review.
private const val MIN_LAUNCHES_BEFORE_REVIEW = 5

// Number of days to wait before showing the first review prompt.
private val INITIAL_REVIEW_DELAY = 3.days

// The maximum number of distinct promotional messages to display to the user.
private val MAX_PROMO_MESSAGES = 2

// The number of app launches to skip between showing consecutive promotional messages.
// After each promotional message is shown, the app will skip this many launches before
// potentially showing another promotional message.
private val PROMO_SKIP_LAUNCHES = 2

// Minimum number of days between subsequent review prompts.
// Since we cannot confirm if the user actually left a review, we use this interval
// to avoid prompting too frequently.
private val STANDARD_REVIEW_DELAY = 5.days
private val KEY_LAST_REVIEW_TIME = longPreferenceKey(TAG + "_last_review_time", 0)
private val KEY_APP_VERSION_CODE = intPreferenceKey(TAG + "_app_version_code", -1)

@Composable
private inline fun <S, O> Preferences.observeAsState(key: Key<S, O>): State<O?> {
    val flow = when (key) {
        is Key1 -> observe(key)
        is Key2 -> observe(key)
    }

    val first = remember(key.name) {
        runBlocking { flow.first() }
    }
    return flow.collectAsState(initial = first)
}

/**
 * @property inAppUpdateProgress A simple property that represents the progress of the in-app update.
 *        The progress value is a float between 0.0 and 1.0, indicating the percentage of the
 *        update that has been completed. The Float.NaN represents a default value when no update
 *        is going on.
 */
@Stable
class MainActivity : ComponentActivity(), SystemFacade, NavDestListener {
    private val snackbarHostState: SnackbarHostState by inject()
    private val preferences: Preferences by inject()
    private var navController: NavHostController? = null
    private val analytics: Analytics by inject()
    val paymaster by lazy {
        Paymaster(this, BuildConfig.PLAY_CONSOLE_APP_RSA_KEY, Paymaster.products)
    }

    // This needs to be a non- private; because i require this in composable mini-player.
    val remote: Remote by inject()

    //
    private var _style by mutableIntStateOf(WindowStyle.FLAG_STYLE_AUTO)
    override var style: WindowStyle
        get() = WindowStyle(_style)
        set(value) {
            _style = value.value
        }

    var inAppUpdateProgress by mutableFloatStateOf(Float.NaN)
        private set
    val market = AppMarketManager()

    // Observe purchases and prompt the user to install any purchased dynamic features
    private val inAppPurchasesFlow
        get() = paymaster.purchases.onEach { purchases ->
            for (purchase in purchases) {
                // Skip if the purchase is not purchased
                if (!purchase.purchased) continue
                // Update the isAdFreeVersion flag
                if (purchase.id == Paymaster.IAP_NO_ADS) {
                    //isAdFreeVersion = purchase.purchased
                    continue
                }
                val details = paymaster.details.value.find { it.id == purchase.id }
                // Skip if product details are unavailable or the product is not a dynamic feature
                if (details == null || !details.isDynamicFeature) continue
                // Skip if the dynamic feature is already installed
                if (isFeatureInstalled(details.dynamicModuleName)) continue
                // Prompt the user to install the dynamic feature
                val response = snackbarHostState.showSnackbar(
                    resources.getText2(
                        id = Res.string.msg_install_dynamic_module_ss,
                        details.title
                    ),
                    duration = SnackbarDuration.Indefinite,
                    action = resources.getText2(Res.string.install),
                    icon = ImageVector.vectorResource(
                        theme,
                        resources,
                        Res.drawable.ic_apk_install
                    ),
                )
                if (response == SnackbarResult.ActionPerformed)
                    initiateFeatureInstall(details.dynamicModuleName)
            }
        }

    override fun isFeatureInstalled(id: String): Boolean =
        market.isFeatureInstalled(activity = this, name = id)

    override fun initiateFeatureInstall(name: String) {
        lifecycleScope.launch {
            market.initiateFeatureInstall(this@MainActivity, name){result ->
                when (result) {
                    // Update the progress indicator
                    in 0f..1f -> {
                        Log.d("SplitInstall", "Downloading... progress: $result")
                        inAppUpdateProgress = result
                    }
                    AppMarketManager.MODULE_STATE_INSTALLING, AppMarketManager.MODULE_STATE_PENDING -> {
                        // Set the progress to an indeterminate state
                        inAppUpdateProgress = -1f
                        Log.d("SplitInstall", "Installing...")
                    }
                    AppMarketManager.MODULE_STATE_INSTALLED -> {
                        // There is a known issue when observing the state of dynamic module installations.
                        // If the user has requested the installation of the dynamic module during this session,
                        // the inAppTaskProgress flag will not be NaN once the state is reached.
                        // However, if inAppTaskProgress is NaN, it indicates that this callback was triggered due to an app restart,
                        // and no installation request was made in the current session. Therefore, we can safely ignore this state.
                        if (inAppUpdateProgress.isNaN()) return@initiateFeatureInstall
                        // Hide the progress bar
                        inAppUpdateProgress = Float.NaN
                        Log.d("SplitInstall", "Module installed successfully!")
                        // Show a toast message requesting the app restart
                        val res = snackbarHostState.showSnackbar(
                            getString(Res.string.msg_apply_changes_restart),
                            getString(Res.string.restart),
                            duration = SnackbarDuration.Indefinite
                        )
                        // Restart the app if the user chooses to
                        if (res == SnackbarResult.ActionPerformed)
                            restart(true)
                        // The dynamic feature module can now be accessed
                    }
                    AppMarketManager.MODULE_STATE_FAILED, AppMarketManager.MODULE_STATE_UNKNOWN -> {
                        val res = snackbarHostState.showSnackbar(
                            getText(Res.string.msg_unknown_error),

                        )
                    }

                    else -> {
                        // Hide the progress bar for unknown statuses
                        inAppUpdateProgress = Float.NaN
                        Log.d("SplitInstall", "Unknown status: ${result}")
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        // Retrieve the desired font scale from application configuration.
        val scale = AppConfig.fontScale
        // If the scale is invalid (-1f) or the newBase context is null,
        // fallback to the default behavior without applying any font scaling.
        if (scale == -1f || newBase == null) {
            super.attachBaseContext(newBase)
            return
        }
        // Create a new Configuration object based on the resources of the newBase context.
        val config = Configuration(newBase.resources.configuration)
        // Set the fontScale property of the configuration to the desired scale.
        config.fontScale = scale
        // Create a new context with the modified configuration.
        val scaledContext = newBase.createConfigurationContext(config)
        // Call the superclass method with the new scaled context.
        super.attachBaseContext(scaledContext)
    }

    override fun restart(global: Boolean) {
        // Get the launch intent for the app's main activity
        val packageManager = packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)

        // Ensure the intent is not null
        if (intent == null) {
            Log.e("AppRestart", "Unable to restart: Launch intent is null")
            return
        }

        // Get the main component for the restart task
        val componentName = intent.component
        if (componentName == null) {
            Log.e("AppRestart", "Unable to restart: Component name is null")
            return
        }
        // Create the main restart intent and start the activity
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        // Terminate the current process to complete the restart
        if (global) Runtime.getRuntime().exit(0)
        finish()
    }

    override fun showToast(message: String, duration: Int) =
        showPlatformToast(message, duration)

    override fun showToast(message: Int, duration: Int) =
        showPlatformToast(message, duration)

    override fun <T> getDeviceService(name: String): T =
        getSystemService(name) as T

    override fun showSnackbar(
        message: CharSequence,
        icon: ImageVector?,
        accent: Color,
        duration: SnackbarDuration,
    ) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                icon = icon,
                accent = accent,
                duration = duration
            )
        }
    }

    override fun showSnackbar(
        message: Int,
        icon: ImageVector?,
        accent: Color,
        duration: SnackbarDuration,
    ) = showSnackbar(
        resources.getText2(id = message),
        icon = icon,
        accent = accent,
        duration = duration
    )

    @Composable
    @NonRestartableComposable
    override fun <S, O> observeAsState(key: Key1<S, O>) =
        preferences.observeAsState(key = key)

    @Composable
    @NonRestartableComposable
    override fun <S, O> observeAsState(key: Key2<S, O>) =
        preferences.observeAsState(key = key) as State<O>

    @Composable
    @NonRestartableComposable
    override fun observePurchaseAsState(id: String): State<Purchase?> {
        return produceState(remember(id) { paymaster.purchases.value.find { it.id == id } }) {
            paymaster.purchases.map { it.find { it.id == id } }.collect {
                value = it  // updating purchase
            }
        }
    }

    override fun launch(intent: Intent, options: Bundle?) =
        startActivity(intent, options)

    override fun initiatePurchaseFlow(id: String) =
        paymaster.initiatePurchaseFlow(this, id)

    override fun getProductInfo(id: String): Product? =
        paymaster.details.value.find { it.id == id }

    override fun initiateUpdateFlow(report: Boolean) {
        lifecycleScope.launch {
            market.initiateUpdateFlow(this@MainActivity){ result ->
                return@initiateUpdateFlow when(result){
                    AppMarketManager.UPDATE_NOT_AVAILABLE -> {
                        if (report) showToast(Res.string.msg_no_new_update_available)
                        AppMarketManager.ACTION_IGNORE
                    }

                    AppMarketManager.UPDATE_NOT_SUPPORTED -> {
                        if (report) showToast("Non‑market installations do not support the in‑app update feature.")
                        /*No-op*/
                        AppMarketManager.ACTION_IGNORE
                    }

                    AppMarketManager.UPDATE_DOWNLOADED -> {
                        // else show the toast.
                        val res = snackbarHostState.showSnackbar(
                            message = resources.getText2(R.string.msg_new_update_downloaded),
                            action = resources.getText2(R.string.install),
                            duration = SnackbarDuration.Long,
                            icon = ImageVector.vectorResource(theme, resources, Res.drawable.ic_downloading)
                        )
                        // complete update when ever user clicks on action.
                        if (res == SnackbarResult.ActionPerformed) AppMarketManager.ACTION_INSTALL
                        else AppMarketManager.ACTION_IGNORE
                    }
                    // progress
                    else -> {
                        inAppUpdateProgress = result
                        Log.d(TAG, "initiateUpdateFlow: $result")
                        AppMarketManager.ACTION_IGNORE
                    }
                }
            }
        }

    }

    override fun <S, O> setPreference(key: Key<S, O>, value: O) {
        preferences[key] = value
    }

    override fun initiateReviewFlow() {
        lifecycleScope.launch {
            // Get the app launch count from preferences.
            val count = preferences[Settings.KEY_LAUNCH_COUNTER]
            // Check if the minimum launch count has been reached.
            if (count < MIN_LAUNCHES_BEFORE_REVIEW)
                return@launch

            // Get the last time the review prompt was shown.
            // Check if enough time has passed since the last review prompt.
            val currentTime = System.currentTimeMillis()
            val lastAskedTime = preferences[KEY_LAST_REVIEW_TIME]
            if (currentTime - lastAskedTime <= STANDARD_REVIEW_DELAY.inWholeMilliseconds)
                return@launch

            // Request and launch the review flow.
            runCatching(TAG) {
                // Update the last asked time in preferences
                preferences[KEY_LAST_REVIEW_TIME] = System.currentTimeMillis()
                market.initiateReviewFlow(this@MainActivity)
                // Optionally log an event to Firebase Analytics.
                // host.fAnalytics.logReviewPromptShown()
            }
        }
    }

    private fun showPromoToast(counter: Int) {
        // TODO - Rename arg counter with something more meaningful.
        // Determine promo category and index.
        //
        // Formula:
        //   index = (category * 1000) + promoInvocationCount
        //
        // Breakdown:
        //   • category = promoInvocationCount % 3
        //       - 0 → In-app purchase promos
        //       - 1 → Featured app promos
        //       - 2 → Tip of the day promos
        //
        //   • promoInvocationCount → app launch counter, used to vary the specific item
        //                             within a category (ensures rotation and avoids repeats).
        lifecycleScope.launch {
            delay(5.seconds)
            val category = counter % 2 // counter % categories count
            val index = (category * 1000) + (counter / 2) % 1000 //
            Log.d(TAG, "showPromoToast: category: $category index: $index")
            // calculate variable index and attempts
            var currentIndex = index;
            var attempts = 0
            while (attempts++ < 30) {
                when (currentIndex) {
                    // Case → In-app purchase promotions
                    in 0..999 -> {
                        val ids = Paymaster.featuredProducts
                        val id = ids[currentIndex % ids.size]
                        Log.d(TAG, "showPromoToast: IAP: $id")
                        // Retrieve purchase info; if missing, skip to next promo
                        val (info, purchase) = paymaster[id] ?: run {
                            currentIndex++   // skip to next promo
                            continue
                        }
                        // Skip if purchased, not purchasable, or freemium
                        if (purchase.purchased || !info.isPurchasable || info.isFreemium) {
                            currentIndex++
                            continue
                        }
                        // Show toast with item description and action
                        val result = snackbarHostState.showSnackbar(
                            info.richDesc,
                            getText(info.action),
                            duration = SnackbarDuration.Indefinite,
                            icon = ImageVector.vectorResource(theme, resources, Res.drawable.ic_hotel_class_outline)
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            initiatePurchaseFlow(id)
                        }
                        return@launch
                    }
                    // Case → Featured apps promotions
                    in 1000..1999 -> { return@launch }
                    else -> { return@launch }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: $intent")
        // Check if the intent action is not ACTION_VIEW; if so, return.
        if (intent.action != Intent.ACTION_VIEW)
            return
        // Obtain the URI from the incoming intent data.
        val data = intent.data ?: return
        // Use a coroutine to handle the media item construction and playback.
        lifecycleScope.launch {
            remote.setMediaItem(data)
            remote.play()
        }
        // FixMe - Don't navigate if this is main intent; as we have already set it as origin.
        navController?.navigate(RouteConsole())
    }

    override fun onResume() {
        super.onResume()
        paymaster.sync()  // trigger sync in paymaster
        lifecycleScope.launch { remote.setAppVisibility(true) }
    }

    override fun onPause() {
        lifecycleScope.launch { remote.setAppVisibility(false) }
        super.onPause()
    }

    override fun onDestroy() {
        paymaster.release() // Destroy it
        super.onDestroy()
    }

    override fun onDestinationChanged(cont: NavController, dest: NavDestination, args: Bundle?) {
        analytics.logEvent(Analytics.EVENT_SCREEN_VIEW) {
            // create params for the event.
            val domain = dest.domain ?: "unknown"
            putString(Analytics.PARAM_SCREEN_NAME, domain)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        // This is determined by checking if savedInstanceState is null.
        // If null, it's a cold start (first time launch or activity recreated from scratch)
        val isColdStart = savedInstanceState == null
        // Configure the splash screen for the app
        initSplashScreen()
        // Initialize
        if (isColdStart) {
            // Wait for Splash Anim
            if (AppConfig.isSplashAnimWaitEnabled) {
                val uptimeMillis = SystemClock.uptimeMillis()
                val content = findViewById<View>(android.R.id.content)
                val onPreDrawListener = object : OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        // wait for splash screen animation to finish.
                        val finished =
                            SystemClock.uptimeMillis() - uptimeMillis >= 1500 // maxDuration.
                        Log.d(TAG, "onPreDraw: $finished")
                        if (finished)
                            content.viewTreeObserver.removeOnPreDrawListener(this)
                        return finished
                    }
                }
                content.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
            }
            // Trigger update flow
            initiateUpdateFlow()
            // show promo message
            // update the state of variables dependent on payment master.
            // Observe active purchases and prompt the user to install any purchased dynamic features.
            inAppPurchasesFlow.launchIn(lifecycleScope)
            // Handle pending intents after a brief delay to ensure UI readiness
            // TODO: Replace this with new approach
            lifecycleScope.launch {
                // Introducing a delay of 1000 milliseconds (1 second) here is essential
                // to ensure that the UI is fully prepared to receive the intent.
                // This delay gives the UI components time to initialize and be ready
                // to handle the incoming intent without any potential issues.
                delay(1000)
                onNewIntent(intent)
            }
            // Promote media player on every 5th launch
            // TODO - properly handle promotional content.
            lifecycleScope.launch {
                // Show "What's New" message if the app version has changed
                val versionCode = AppConfig.VERSION_CODE
                val savedVersionCode = preferences[KEY_APP_VERSION_CODE]
                // Update review-time to current time if this is a new install.
                if (savedVersionCode == -1)
                    preferences[KEY_LAST_REVIEW_TIME] = System.currentTimeMillis()
                if (savedVersionCode != versionCode) {
                    preferences[KEY_APP_VERSION_CODE] = versionCode
                    delay(10.seconds) // delay for 10 sec
                    showSnackbar(
                        Res.string.release_notes,
                        duration = SnackbarDuration.Indefinite,
                        icon = ImageVector.vectorResource(
                            theme,
                            resources,
                            Res.drawable.ic_downloading
                        )
                    ) // What's new
                    return@launch
                }

                // Promotional messages are displayed only after the app has been launched
                // more than 5 times (MIN_LAUNCHES_BEFORE_REVIEW).
                // This ensures that users have had a chance to familiarize themselves with the app
                // before being presented with these messages.
                // An index of 0 is reserved for the "What's New" message and is handled separately.
                // Promotional messages start with index 1.
                // The index is calculated using the formula: (counter % MAX_PROMO_MESSAGES).coerceAtLeast(1).
                // Each message is skipped by PROMO_SKIP_LAUNCHES number of launches.
                val counter = preferences[Settings.KEY_LAUNCH_COUNTER]
                if (counter < MIN_LAUNCHES_BEFORE_REVIEW)
                    return@launch
                val newCounter = counter - MIN_LAUNCHES_BEFORE_REVIEW
                val interval = PROMO_SKIP_LAUNCHES + 1
                // This line calculates which promotional message to show from a rotating set.
                Log.d(
                    TAG,
                    "Promo(counter=$counter," +
                            " interval=$interval," +
                            " newCounter=$newCounter," +
                            " skip = ${newCounter % interval}," +
                            " index = ${(newCounter / interval) % MAX_PROMO_MESSAGES + 1} ) "
                )
                if (interval == 0 || newCounter % interval == 0) {
                    val index = (newCounter / interval) % MAX_PROMO_MESSAGES + 1
                    Log.d(TAG, "onCreate: $index")
                    showPromoToast(newCounter)
                }
            }
        }
        // Set up the window to fit the system windows
        // This setting is usually configured in the app theme, but is ensured here
        WindowCompat.enableEdgeToEdge(window)
        // Set the content of the activity
        setContent {
            val navController = rememberNavController()
            // If the action is VIEW, load the content first, regardless
            // of whether the app is currently locked or not. This allows
            // users to view shared media directly.
            // else If authentication is required, move to the lock screen
            Home(
                when {
                    intent.action == Intent.ACTION_VIEW -> RouteConsole
                    else -> RouteLibrary
                },
                snackbarHostState,
                navController
            )
            // Manage lifecycle-related events and listeners
            DisposableEffect(Unit) {
                navController.addOnDestinationChangedListener(this@MainActivity)
                // Cover the screen with lock_screen if authentication is required
                // Only remove this veil when the user authenticates
                // don't show lock screen because their is dedicated button
                // if (isAuthenticationRequired) unlock();
                this@MainActivity.navController = navController
                onDispose {
                    navController.removeOnDestinationChangedListener(this@MainActivity)
                    this@MainActivity.navController = null
                }
            }
        }
    }
}