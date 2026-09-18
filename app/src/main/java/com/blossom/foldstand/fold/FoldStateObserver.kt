package com.blossom.foldstand.fold

import android.app.Activity
import android.content.Context
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.blossom.foldstand.domain.FoldBounds
import com.blossom.foldstand.domain.FoldOrientation
import com.blossom.foldstand.domain.FoldPosture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FoldStateObserver(
    context: Context,
    activity: Activity,
) {
    val posture: Flow<FoldPosture> = WindowInfoTracker.getOrCreate(context)
        .windowLayoutInfo(activity)
        .map { layoutInfo ->
            val feature = layoutInfo.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull()
                ?: return@map FoldPosture.Unknown
            feature.toPosture()
        }
        .distinctUntilChanged()
}

private fun FoldingFeature.toPosture(): FoldPosture {
    val foldBounds = FoldBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    val foldOrientation = when (orientation) {
        FoldingFeature.Orientation.HORIZONTAL -> FoldOrientation.Horizontal
        FoldingFeature.Orientation.VERTICAL -> FoldOrientation.Vertical
        else -> return FoldPosture.Unknown
    }
    return when (state) {
        FoldingFeature.State.HALF_OPENED -> FoldPosture.HalfOpened(
            orientation = foldOrientation,
            bounds = foldBounds,
            isSeparating = isSeparating,
            isOccluding = occlusionType == FoldingFeature.OcclusionType.FULL,
        )
        FoldingFeature.State.FLAT -> FoldPosture.Flat(
            orientation = foldOrientation,
            bounds = foldBounds,
            isSeparating = isSeparating,
        )
        else -> FoldPosture.Unknown
    }
}
