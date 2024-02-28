package com.weatherxm.ui.rewarddetails

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.RewardsUseCase

class RewardDetailsViewModel(private val usecase: RewardsUseCase) : ViewModel() {
    fun getRewardsHideAnnotationThreshold() = usecase.getRewardsHideAnnotationThreshold()
}
