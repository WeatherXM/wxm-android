package com.weatherxm.ui.updateprompt

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.databinding.ActivityUpdatePromptBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setHtml
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class UpdatePromptActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityUpdatePromptBinding
    private val navigator: Navigator by inject()
    private val model: UpdatePromptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            model.checkIfLoggedIn()
        }

        binding.updateDescription.setHtml(R.string.desc_update_prompt)

        model.isLoggedIn().observe(this) { result ->
            result.mapLeft {
                Timber.d("Not logged in. Show explorer.")
                navigator.showExplorer(this)
            }.map { username ->
                Timber.d("Already logged in as $username")
                navigator.showHome(this)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        if (model.isUpdateMandatory()) {
            binding.toolbar.navigationIcon = null
            binding.continueWithoutUpdatingBtn.visibility = View.GONE
        }

        binding.updateBtn.setOnClickListener {
            navigator.openPlayStore(this, getString(R.string.market_url, packageName))
        }

        binding.continueWithoutUpdatingBtn.setOnClickListener {
            model.checkIfLoggedIn()
        }

        binding.changelog.text = model.getChangelog()
    }

    override fun onBackPressed() {
        if (model.isUpdateMandatory()) {
            super.onBackPressed()
        } else {
            model.checkIfLoggedIn()
        }
    }
}
