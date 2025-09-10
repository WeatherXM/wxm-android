package com.weatherxm.ui.onboarding

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.databinding.ActivityOnboardingBinding
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.compose.LargeText
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnboardingActivity : BaseActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private val model: OnboardingViewModel by viewModel()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.content.setContent {
            Content(
                onSignup = {
                    model.disableShouldShowOnboarding()
                    navigator.showSignup(this, true)
                    finish()
                },
                onExploreTheApp = {
                    model.disableShouldShowOnboarding()
                    navigator.showAnalyticsOptIn(this)
                    finish()
                }
            )
        }
    }
}

@Suppress("FunctionNaming", "LongMethod")
@Composable
@Preview
private fun Content(
    onSignup: () -> Unit = {},
    onExploreTheApp: () -> Unit = {}
) {
    val images = listOf(
        R.drawable.onboarding_image_1,
        R.drawable.onboarding_image_2,
        R.drawable.onboarding_image_3,
        R.drawable.onboarding_image_4,
        R.drawable.onboarding_image_5
    )
    val texts = listOf(
        R.string.forecasts_every_corner,
        R.string.live_transparent_network,
        R.string.contribute_earn_rewards,
        R.string.community_powered_weather,
        R.string.local_weather_data_global_impact
    )

    val pagerState = rememberPagerState(pageCount = { images.size })
    val currentImage = remember { derivedStateOf { images[pagerState.currentPage] } }

    Box {
        Box(modifier = Modifier.blur(100.dp)) {
            Crossfade(targetState = currentImage.value) { imageRes ->
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    painter = painterResource(imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Transparent
                        )
                    )
                )
        )
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 40.dp,
                        horizontal = dimensionResource(R.dimen.padding_normal_to_large)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painter = painterResource(R.drawable.full_logo), contentDescription = null)
                Text(
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_small)),
                    text = stringResource(R.string.real_weather_real_rewards),
                    fontSize = 20.sp,
                    color = colorResource(R.color.dark_dark_grey),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val padding = (screenWidth - 300.dp) / 2
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(300.dp),
                contentPadding = PaddingValues(horizontal = padding),
                pageSpacing = dimensionResource(R.dimen.padding_normal_to_large),
                modifier = Modifier
                    .weight(1F)
                    .fillMaxWidth()
            ) { page ->
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.shadow(
                        elevation = dimensionResource(R.dimen.elevation_normal),
                        shape = RoundedCornerShape(
                            dimensionResource(R.dimen.radius_extra_large)
                        )
                    )
                ) {
                    Image(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .clip(
                                RoundedCornerShape(
                                    dimensionResource(R.dimen.radius_extra_large)
                                )
                            ),
                        painter = painterResource(images[page]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .height(120.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = dimensionResource(R.dimen.radius_extra_large),
                                    bottomEnd = dimensionResource(R.dimen.radius_extra_large)
                                )
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black
                                    )
                                )
                            )
                    )
                    Text(
                        modifier = Modifier.padding(
                            bottom = dimensionResource(R.dimen.padding_large),
                            start = dimensionResource(R.dimen.padding_normal),
                            end = dimensionResource(R.dimen.padding_normal)
                        ),
                        text = stringResource(texts[page]),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorResource(R.color.dark_text),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier.padding(
                    vertical = 40.dp,
                    horizontal = dimensionResource(R.dimen.padding_normal)
                )
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.padding_normal)),
                    onClick = onSignup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.light_top),
                        contentColor = colorResource(R.color.light_text)
                    ),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
                ) {
                    LargeText(
                        text = stringResource(R.string.sign_up_now),
                        colorRes = R.color.light_text,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.padding_normal)),
                    onClick = onExploreTheApp,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium))
                ) {
                    LargeText(
                        text = stringResource(R.string.explore_the_app),
                        colorRes = R.color.dark_text,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
