package com.lockr.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(modifier.fillMaxSize().background(LockrColors.Pearl)) {
        AuroraBackdrop()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            OnboardingPageContent(
                page = page,
                currentPage = pagerState.currentPage,
                onBack = { scope.launch { pagerState.animateScrollToPage((page - 1).coerceAtLeast(0)) } },
                onSkip = onFinish,
                onNext = {
                    if (page == onboardingPages.lastIndex) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(page + 1) }
                }
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: Int,
    currentPage: Int,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val data = onboardingPages[page]
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(data.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = 0.62f),
                        0.24f to Color.White.copy(alpha = 0.30f),
                        0.52f to Color.Transparent,
                        0.80f to Color.Transparent,
                        1f to Color.White.copy(alpha = 0.42f)
                    )
                )
            )
        )

        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().height(54.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (page == 0) {
                    Spacer(Modifier.size(42.dp))
                } else {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.75f))
                            .border(1.dp, Color.White.copy(alpha = 0.92f), CircleShape).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous page", tint = LockrColors.TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                Text(
                    if (page == onboardingPages.lastIndex) "Get Started" else "Skip",
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onSkip).padding(horizontal = 8.dp, vertical = 9.dp),
                    color = LockrColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            Column(Modifier.padding(top = 12.dp)) {
                Text(
                    data.title,
                    color = LockrColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.8).sp
                    )
                )
                Text(
                    data.description,
                    color = LockrColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 17.sp),
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            Spacer(Modifier.weight(1f))
            OnboardingFooter(currentPage = currentPage, page = page, onNext = onNext)
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
            Modifier.size(78.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val arcTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                onboardingPages.indices.forEach { index ->
                    drawArc(
                        color = if (index <= currentPage) LockrColors.Accent else Color(0x77738497),
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
                Modifier.size(54.dp)
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
                    tint = LockrColors.TextPrimary,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}
