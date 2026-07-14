package com.zs.core.market

import android.app.Activity
import com.zs.core.AppMarketManagerImpl
import com.zs.core.market.AppMarketManager.Companion.ACTION_IGNORE
import com.zs.core.market.AppMarketManager.Companion.ACTION_INSTALL
import com.zs.core.market.AppMarketManager.Companion.UPDATE_DOWNLOADED
import com.zs.core.market.AppMarketManager.Companion.UPDATE_NOT_AVAILABLE
import com.zs.core.market.AppMarketManager.Companion.UPDATE_NOT_SUPPORTED

/**
 * Interface defining the contract for managing app market interactions, including in-app updates,
 * in-app reviews, and dynamic feature module management.
 *
 * ### Update State Constants:
 * * [UPDATE_NOT_AVAILABLE]: Indicates no update is currently available for the application.
 * * [UPDATE_DOWNLOADED]: Indicates the update has been successfully downloaded and is ready for installation.
 * * [UPDATE_NOT_SUPPORTED]: Indicates that in-app updates are not supported on the current device or market configuration.
 *
 * ### Feature Install State Constants:
 * * [FEATURE_INSTALLING]: The dynamic feature is currently being downloaded or installed.
 * * [FEATURE_PENDING]: The request for feature installation is pending processing.
 */
interface AppMarketManager {

    companion object {
        const val UPDATE_NOT_AVAILABLE = -1f
        const val UPDATE_DOWNLOADED = -2f
        const val UPDATE_NOT_SUPPORTED = -4f

        // in-app feature update
        const val MODULE_STATE_INSTALLING = -1f
        const val MODULE_STATE_PENDING = -2f
        const val MODULE_STATE_INSTALLED = -3f
        const val MODULE_STATE_FAILED = -4f
        const val MODULE_STATE_UNKNOWN = -5f

        // In-app update and review settings
        // Maximum staleness days allowed for a flexible update.
        // If the app is older than this, an immediate update will be enforced.
        const val FLEXIBLE_UPDATE_MAX_STALENESS_DAYS = 2

        const val ACTION_IGNORE = 0
        const val ACTION_INSTALL = 1

        operator fun invoke(): AppMarketManager = AppMarketManagerImpl()
    }

    /**
     * Checks if a specific dynamic feature module is currently installed on the device.
     *
     * @param activity The current activity context used to check the installation status.
     * @param name The unique identifier or name of the feature module.
     * @return `true` if the feature is already installed and available; `false` otherwise.
     */
    fun isFeatureInstalled(activity: Activity, name: String): Boolean
    /**
     * Initiates the installation process for a dynamic feature module.
     *
     * This method handles the request to download and install a feature by its [name].
     * Progress and status updates are communicated via the [provider] callback using
     * the feature-related constants (e.g., [FEATURE_INSTALLING], [FEATURE_INSTALLED]).
     *
     * @param activity The current activity context used to launch the installation flow.
     * @param name The unique identifier/name of the dynamic feature module to be installed.
     * @param provider A suspending callback that receives the current installation status
     * or progress as a [Float] and returns an integer action code (e.g., [ACTION_INSTALL]
     * or [ACTION_IGNORE]) to determine how the flow should proceed.
     */
    suspend fun initiateFeatureInstall(activity: Activity, name: String, provider: suspend (result: Float) -> Unit)
    /**
     * Initiates the in-app review flow, allowing the user to rate and review the app
     * without leaving the application.
     *
     * Note: The review dialog is subject to quota limits and may not always be
     * displayed even if the request is successful.
     *
     * @param activity The current activity context used to launch the review dialog.
     */
    suspend fun initiateReviewFlow(activity: Activity)
    /**
     * Initiates the in-app update flow, checking for available updates and handling the download process.
     *
     * @param activity The current activity context used to launch the update UI.
     * @param provider A suspending callback that reports the update status or progress as a [Float].
     * The callback should return an [Int] representing the desired action to take (e.g., [ACTION_INSTALL] or [ACTION_IGNORE]).
     * Status values may include [UPDATE_NOT_AVAILABLE], [UPDATE_DOWNLOADED], [UPDATE_NOT_SUPPORTED], or a progress percentage (0.0 to 1.0).
     */
    suspend fun initiateUpdateFlow(activity: Activity, provider: suspend (result: Float) -> Int)
}