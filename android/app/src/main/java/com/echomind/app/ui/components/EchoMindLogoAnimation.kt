package com.echomind.app.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.echomind.app.R

@Composable
fun EchoMindLogoAnimation(
    modifier: Modifier = Modifier,
    width: Dp = 240.dp,
    size: Dp = width,
    speed: Float = 1.0f,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true
) {
    val context = LocalContext.current
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.echo_mind_logo)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        speed = speed,
        iterations = iterations
    )

    val fontMap = remember(context) {
        val caveatTf = try {
            Typeface.createFromAsset(context.assets, "fonts/Caveat-Bold.ttf")
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }
        val calSansTf = try {
            Typeface.createFromAsset(context.assets, "fonts/Cal Sans-Regular.ttf")
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }
        mapOf(
            "Caveat Bold" to caveatTf,
            "Caveat" to caveatTf,
            "Caveat-Bold" to caveatTf,
            "Cal Sans Regular" to calSansTf,
            "Cal Sans" to calSansTf,
            "Cal Sans-Regular" to calSansTf
        )
    }

    val actualWidth = if (width != 240.dp) width else size

    LottieAnimation(
        composition = composition,
        progress = { progress },
        fontMap = fontMap,
        renderMode = RenderMode.AUTOMATIC,
        modifier = modifier
            .width(actualWidth)
            .aspectRatio(428f / 123f)
    )
}
