package com.weatherxm.ui.home.locations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.datasource.RemoteBannersDataSourceImpl.Companion.ANNOUNCEMENT_LOCAL_PRO_ACTION_URL
import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.databinding.FragmentLocationsHomeBinding
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.setCardRadius
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.ProPromotionDialogFragment
import com.weatherxm.ui.components.compose.AnnouncementBannerView
import com.weatherxm.ui.components.compose.InfoBannerView
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.ui.home.devices.DevicesViewModel
import com.weatherxm.util.NumberUtils.formatTokens
import dev.chrisbanes.insetter.applyInsetter
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LocationsFragment : BaseFragment() {
    private val parentModel: HomeViewModel by activityViewModel()
    private val devicesModel: DevicesViewModel by activityViewModel()
    private lateinit var binding: FragmentLocationsHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLocationsHomeBinding.inflate(inflater, container, false)

        binding.root.applyInsetter {
            type(statusBars = true) {
                padding(left = false, top = true, right = false, bottom = false)
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            parentModel.getRemoteBanners()
        }

        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            parentModel.onScroll(scrollY - oldScrollY)
        }

        devicesModel.onDevicesRewards().observe(viewLifecycleOwner) {
            onDevicesRewards(it)
        }

        parentModel.onInfoBanner().observe(viewLifecycleOwner) {
            onInfoBanner(it)
        }

        parentModel.onAnnouncementBanner().observe(viewLifecycleOwner) {
            onAnnouncementBanner(it)
        }

        return binding.root
    }

    private fun onInfoBanner(infoBanner: RemoteBanner?) {
        if (infoBanner != null) {
            binding.infoBanner.setContent {
                InfoBannerView(
                    title = infoBanner.title,
                    subtitle = infoBanner.message,
                    actionLabel = infoBanner.actionLabel,
                    showActionButton = infoBanner.showActionButton,
                    showCloseButton = infoBanner.showCloseButton,
                    onAction = {
                        analytics.trackEventSelectContent(
                            AnalyticsService.ParamValue.INFO_BANNER_BUTTON.paramValue,
                            Pair(FirebaseAnalytics.Param.ITEM_ID, infoBanner.url)
                        )
                        navigator.openWebsite(context, infoBanner.url)
                    },
                    onClose = {
                        parentModel.dismissRemoteBanner(RemoteBannerType.INFO_BANNER, infoBanner.id)
                        binding.contentContainerCard.setCardRadius(0F, 0F, 0F, 0F)
                        binding.infoBanner.visible(false)
                    }
                )
            }
            binding.infoBanner.visible(true)
            val radius = resources.getDimension(R.dimen.radius_large)
            binding.contentContainerCard.setCardRadius(radius, radius, 0F, 0F)
        } else if (binding.infoBanner.isVisible) {
            binding.infoBanner.visible(false)
            binding.contentContainerCard.setCardRadius(0F, 0F, 0F, 0F)
        }
    }

    private fun onAnnouncementBanner(announcementBanner: RemoteBanner?) {
        announcementBanner?.let {
            binding.announcementBanner.setContent {
                AnnouncementBannerView(
                    title = it.title,
                    subtitle = it.message,
                    actionLabel = it.actionLabel,
                    showActionButton = it.showActionButton,
                    showCloseButton = it.showCloseButton,
                    onAction = {
                        analytics.trackEventSelectContent(
                            AnalyticsService.ParamValue.ANNOUNCEMENT_CTA.paramValue,
                            Pair(FirebaseAnalytics.Param.ITEM_ID, it.url),
                        )
                        if (it.url == ANNOUNCEMENT_LOCAL_PRO_ACTION_URL) {
                            analytics.trackEventSelectContent(
                                AnalyticsService.ParamValue.PRO_PROMOTION_CTA.paramValue,
                                Pair(FirebaseAnalytics.Param.ITEM_ID, it.url),
                                Pair(
                                    FirebaseAnalytics.Param.SOURCE,
                                    AnalyticsService.ParamValue.REMOTE_DEVICES_LIST.paramValue
                                )
                            )
                            ProPromotionDialogFragment().show(this)
                        } else {
                            navigator.openWebsite(context, it.url)
                        }
                    },
                    onClose = {
                        parentModel.dismissRemoteBanner(RemoteBannerType.ANNOUNCEMENT, it.id)
                        binding.announcementBanner.visible(false)
                    }
                )
            }
            binding.announcementBanner.visible(true)
        } ?: binding.announcementBanner.visible(false)
    }

    private fun onDevicesRewards(rewards: DevicesRewards) {
        binding.totalEarnedCard.visible(true)
        binding.totalEarnedCard.setOnClickListener {
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.TOKENS_EARNED_PRESSED.paramValue
            )
            navigator.showDevicesRewards(this, rewards)
        }
        binding.stationRewards.text = getString(R.string.wxm_amount, formatTokens(rewards.total))
        binding.totalEarnedContainer.visible(rewards.total > 0F)
        binding.noRewardsYet.visible(rewards.devices.isNotEmpty() && rewards.total == 0F)
        binding.ownDeployEarn.visible(rewards.devices.isEmpty() && rewards.total == 0F)
    }
}
