package com.android.intentresolver.v2

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import com.android.intentresolver.ChooserRequestParameters
import com.android.intentresolver.icons.TargetDataLoader

/** Activity logic for [ChooserActivity]. */
class ChooserActivityLogic(
    private val tag: String,
    activityProvider: () -> ComponentActivity,
    targetDataLoaderProvider: () -> TargetDataLoader,
    private val onPreInitialization: () -> Unit,
) : ActivityLogic, CommonActivityLogic by CommonActivityLogicImpl(activityProvider) {

    override val targetIntent: Intent by lazy { chooserRequestParameters?.targetIntent ?: Intent() }

    override val resolvingHome: Boolean = false

    override val additionalTargets: List<Intent>? by lazy {
        chooserRequestParameters?.additionalTargets?.toList()
    }

    override val title: CharSequence? by lazy { chooserRequestParameters?.title }

    override val defaultTitleResId: Int by lazy {
        chooserRequestParameters?.defaultTitleResource ?: 0
    }

    override val initialIntents: List<Intent>? by lazy {
        chooserRequestParameters?.initialIntents?.toList()
    }

    override val supportsAlwaysUseOption: Boolean = false

    override val targetDataLoader: TargetDataLoader by lazy { targetDataLoaderProvider() }

    val chooserRequestParameters: ChooserRequestParameters? by lazy {
        try {
            ChooserRequestParameters(
                (activity as Activity).intent,
                referrerPackageName,
                (activity as Activity).referrer,
            )
        } catch (e: IllegalArgumentException) {
            Log.e(tag, "Caller provided invalid Chooser request parameters", e)
            null
        }
    }

    override fun preInitialization() {
        onPreInitialization()
    }
}
