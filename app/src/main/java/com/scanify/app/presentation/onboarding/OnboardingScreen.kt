package com.scanify.app.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.scanify.app.R
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.onboarding.components.PagerScreen
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    @param:DrawableRes val imageRes: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    onOnboardingFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Scan In A Snap",
            description = "Turn your notes, and any documents into crisp PDFs in seconds.",
            imageRes = R.drawable.onboarding1
        ),
        OnboardingPage(
            title = "No Watermarks",
            description = "Clean, professional PDFs without branding or hidden costs.",
            imageRes = R.drawable.onboarding2
        ),
        OnboardingPage(
            title = "100% Private & Offline",
            description = "Your data stays exactly where it belongs-on your device. No cloud uploads, no tracking.",
            imageRes = R.drawable.onboarding3
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { position ->
            PagerScreen(onBoardingPage = pages[position])
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == iteration) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        onOnboardingFinished()
                        navController.navigate(Routes.HomeScreen) {
                            popUpTo(Routes.OnBoardingScreen) { inclusive = true }
                        }
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(modifier=Modifier.height(24.dp))
    }
}