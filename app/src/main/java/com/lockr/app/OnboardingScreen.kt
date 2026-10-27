package com.lockr.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val image: Int,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        image = R.drawable.onboarding_vault,
        title = "Everything, together.",
        description = "Your personal vault for everyday essentials."
    ),
    OnboardingPage(
        image = R.drawable.onboarding_privacy,
        title = "Private by design.",
        description = "Encrypted and stored only on this device."
    ),
    OnboardingPage(
        image = R.drawable.onboarding_access,
        title = "Ready when you are.",
        description = "Your private vault is always available offline."
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val motionEnabled = LocalMotionEnabled.current
    val navigate: (Int) -> Unit = { target ->
        if (!pagerState.isScrollInProgress) scope.launch {
            if (!motionEnabled) pagerState.scrollToPage(target.coerceIn(0, onboardingPages.lastIndex))
            else pagerState.animateScrollToPage(target.coerceIn(0, onboardingPages.lastIndex),
                animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
    }
    BackHandler(enabled = pagerState.currentPage > 0) { navigate(pagerState.currentPage - 1) }
    Box(modifier.fillMaxSize().background(LockrColors.Pearl)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 2) { page ->
            val data = onboardingPages[page]
            Box(Modifier.fillMaxSize()) {
                Image(painterResource(data.image), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                    Color.White.copy(alpha = 0.75f), Color.Transparent, Color.White.copy(alpha = 0.3f)))))
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 78.dp)) {
                    Text(data.title, color = Color(0xFF182333),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp,
                            lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp))
                    Text(data.description, color = Color(0xFF66758C),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
                        modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
        // Navigation stays still while the artwork and copy slide together.
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                if (pagerState.currentPage > 0) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.75f))
                        .clickable(enabled = !pagerState.isScrollInProgress) { navigate(pagerState.currentPage - 1) },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.ChevronLeft, "Previous page", tint = Color(0xFF182333), modifier = Modifier.size(20.dp))
                    }
                } else Spacer(Modifier.size(42.dp))
                if (pagerState.currentPage < onboardingPages.lastIndex) {
                    Text("Skip", Modifier.clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !pagerState.isScrollInProgress, onClick = onFinish)
                        .padding(horizontal = 8.dp, vertical = 9.dp), color = Color(0xFF182333))
                }
            }
            Spacer(Modifier.weight(1f))
            OnboardingFooter(pagerState.currentPage, pagerState.currentPage) {
                if (!pagerState.isScrollInProgress) {
                    if (pagerState.currentPage == onboardingPages.lastIndex) onFinish()
                    else navigate(pagerState.currentPage + 1)
                }
            }
        }
    }
}

@Composable
private fun OnboardingFooter(currentPage: Int, page: Int, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        val buttonColors = when (page) {
            0 -> listOf(Color(0xFFA9ECE7), Color(0xFFD4FBF5))
            1 -> listOf(Color(0xFF9BD2FF), Color(0xFFD9F1FF))
            else -> listOf(Color(0xFFFFC6B8), Color(0xFFFFEBCF))
        }
        Box(
            Modifier.size(66.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val arcTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                onboardingPages.indices.forEach { index ->
                    drawArc(
                        color = if (index <= currentPage) Color(0xFF398F89) else Color(0x77738497),
                        startAngle = -90f + index * 120f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            val shape = CircleShape
            Box(
                Modifier.size(52.dp)
                    .shadow(9.dp, shape, ambientColor = buttonColors.first().copy(alpha = 0.55f))
                    .clip(shape)
                    .background(Brush.linearGradient(buttonColors))
                    .border(1.dp, Color.White.copy(alpha = 0.96f), shape)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (page == onboardingPages.lastIndex) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = if (page == onboardingPages.lastIndex) "Get started" else "Next page",
                    tint = Color(0xFF182333),
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}
