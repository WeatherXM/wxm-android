package com.weatherxm.ui.home.quests.steps

import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.usecases.QuestsUseCase
import kotlinx.coroutines.CoroutineDispatcher

class GenericStepViewModel(
    val questStep: QuestStep,
    private val usecase: QuestsUseCase,
    private val dispatcher: CoroutineDispatcher): ViewModel() {

}
