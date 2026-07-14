package com.zs.core

import android.app.Activity
import com.zs.core.market.AppMarketManager

internal class AppMarketManagerImpl() : AppMarketManager {
    override fun isFeatureInstalled(
        activity: Activity,
        name: String
    ): Boolean {
        return false
    }

    override suspend fun initiateFeatureInstall(
        activity: Activity,
        name: String,
        provider: suspend (result: Float) -> Unit
    ) {
        provider(AppMarketManager.MODULE_STATE_FAILED)
    }

    override suspend fun initiateReviewFlow(activity: Activity) {
       // no-op
    }

    override suspend fun initiateUpdateFlow(
        activity: Activity,
        provider: suspend (result: Float) -> Int
    ) {
        provider(AppMarketManager.UPDATE_NOT_SUPPORTED)
    }
}