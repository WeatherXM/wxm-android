package com.weatherxm.ui.questcompletion

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.weatherxm.R
import com.weatherxm.databinding.ActivityQuestCompletionBinding
import com.weatherxm.ui.common.Animation
import com.weatherxm.ui.common.Contracts.ARG_QUEST_TITLE
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.show
import com.weatherxm.ui.components.BaseActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuestCompletionActivity : BaseActivity() {
    private lateinit var binding: ActivityQuestCompletionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestCompletionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.continueBtn.setOnClickListener {
            finish()
        }

        binding.shareBtn.setOnClickListener {
            navigator.openShare(this, getString(R.string.quest_completed_share))
        }

        val questTitle = intent.getStringExtra(ARG_QUEST_TITLE) ?: String.empty()
        binding.subtitle.text = getString(R.string.quest_completed_message, questTitle)

        @Suppress("MagicNumber")
        lifecycleScope.launch {
            delay(2000L)
            binding.textsContainer.translationY = binding.textsContainer.height.toFloat()
            binding.buttonsContainer.translationY = binding.buttonsContainer.height.toFloat()
            binding.textsContainer.show(Animation.ShowAnimation.SlideInFromBottom)
            binding.buttonsContainer.show(Animation.ShowAnimation.SlideInFromBottom)
        }
    }
}
