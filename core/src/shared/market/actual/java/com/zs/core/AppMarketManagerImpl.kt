package com.zs.core

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestProgressFlow
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.ktx.requestUpdateFlow
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.zs.core.market.AppMarketManager
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus as Flag


internal class AppMarketManagerImpl() : AppMarketManager {

    var manager: SplitInstallManager? = null

    override fun isFeatureInstalled(activity: Activity, name: String): Boolean {
        val manager = manager ?: SplitInstallManagerFactory.create(activity)
        return manager.installedModules.contains(name)
    }

    override suspend fun initiateFeatureInstall(
        activity: Activity,
        name: String,
        provider: suspend (result: Float) -> Unit
    ) {
        val request = SplitInstallRequest.newBuilder().addModule(name).build()
        val manager = manager ?: SplitInstallManagerFactory.create(activity)
        manager.startInstall(request)
        manager.requestProgressFlow().collect { state ->
            when (state.status()) {
                Flag.DOWNLOADING -> {
                    // Calculate the download progress as a percentage
                    val percent =
                        state.bytesDownloaded().toFloat() / state.totalBytesToDownload()
                    Log.d("SplitInstall", "Download progress: $percent%")
                    // Update the progress indicator
                    provider(percent)
                }

                Flag.INSTALLING -> provider(AppMarketManager.MODULE_STATE_INSTALLING)
                Flag.PENDING -> provider(AppMarketManager.MODULE_STATE_PENDING)
                Flag.INSTALLED -> provider(AppMarketManager.MODULE_STATE_INSTALLED)
                Flag.FAILED -> provider(AppMarketManager.MODULE_STATE_FAILED)
                else -> {
                    provider(AppMarketManager.MODULE_STATE_UNKNOWN)
                    Log.d("SplitInstall", "Unknown status: ${state.status()}")
                }
            }
        }
    }

    override suspend fun initiateReviewFlow(activity: Activity) {
        val reviewManager = ReviewManagerFactory.create(activity)
        val info = reviewManager.requestReview()
        reviewManager.launchReviewFlow(activity, info)
    }

    override suspend fun initiateUpdateFlow(
        activity: Activity,
        provider: suspend (result: Float) -> Int
    ) {
        val manager = AppUpdateManagerFactory.create(activity)
        manager.requestUpdateFlow().collect { result ->
            when (result) {
                is AppUpdateResult.NotAvailable -> provider(AppMarketManager.UPDATE_NOT_AVAILABLE)
                is AppUpdateResult.InProgress -> {
                    val state = result.installState
                    val total = state.totalBytesToDownload()
                    val downloaded = state.bytesDownloaded()
                    val progress = when {
                        total <= 0 -> -1f
                        total == downloaded -> Float.NaN
                        else -> downloaded / total.toFloat()
                    }
                    provider(progress)
                }

                is AppUpdateResult.Available -> {
                    // if user choose to skip the update handle that case also.
                    val isFlexible = (result.updateInfo.clientVersionStalenessDays()
                        ?: -1) <= AppMarketManager.FLEXIBLE_UPDATE_MAX_STALENESS_DAYS
                    if (isFlexible) result.startFlexibleUpdate(
                        activity = activity, 1000
                    )
                    else result.startImmediateUpdate(
                        activity = activity, 1000
                    )

                }

                is AppUpdateResult.Downloaded -> {
                    val info = manager.requestAppUpdateInfo()
                    //when update first becomes available
                    //don't force it.
                    // make it required when staleness days overcome allowed limit
                    val isFlexible = (info.clientVersionStalenessDays()
                        ?: -1) <= AppMarketManager.FLEXIBLE_UPDATE_MAX_STALENESS_DAYS

                    // forcefully update; if it's flexible
                    if (!isFlexible) {
                        manager.completeUpdate()
                        return@collect
                    }

                    val action = provider(AppMarketManager.UPDATE_DOWNLOADED)
                    if (action == AppMarketManager.ACTION_INSTALL)
                        manager.completeUpdate()
                }
            }
        }
    }
}