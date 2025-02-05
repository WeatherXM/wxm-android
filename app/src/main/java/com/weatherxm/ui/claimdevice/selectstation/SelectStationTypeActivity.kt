package com.weatherxm.ui.claimdevice.selectstation

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityClaimSelectStationBinding
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.components.compose.CardViewClickable
import com.weatherxm.ui.components.compose.MediumText

class SelectStationTypeActivity : BaseActivity() {
    private lateinit var binding: ActivityClaimSelectStationBinding

    // Register the launcher for the edit location activity and wait for a possible result
    private val claimingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimSelectStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.deviceTypes.setContent {
            DeviceTypes()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            AnalyticsService.Screen.CLAIM_DEVICE_TYPE_SELECTION, classSimpleName()
        )
    }

    private fun startClaimingFlow(deviceType: DeviceType) {
        navigator.showClaimFlow(claimingLauncher, this, deviceType)
    }

    @Suppress("FunctionNaming")
    @Preview
    @Composable
    fun DeviceTypes() {
        Column(
            verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))
        ) {
            Row(
                horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))
            ) {
                Column(Modifier.weight(1F)) {
                    CardViewClickable(
                        onClickListener = { startClaimingFlow(DeviceType.M5_WIFI) }
                    ) {
                        TypeContent(stringResource(R.string.m5_wifi), R.drawable.device_type_m5)
                    }
                }
                Column(Modifier.weight(1F)) {
                    CardViewClickable(
                        onClickListener = { startClaimingFlow(DeviceType.D1_WIFI) }
                    ) {
                        TypeContent(stringResource(R.string.d1_wifi), R.drawable.device_type_d1)
                    }
                }
            }
            Row(
                horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_normal))
            ) {
                Column(Modifier.weight(1F)) {
                    CardViewClickable(
                        onClickListener = { startClaimingFlow(DeviceType.HELIUM) },
                    ) {
                        TypeContent(stringResource(R.string.helium), R.drawable.device_type_helium)
                    }
                }
                Column(Modifier.weight(1F)) {
                    CardViewClickable(
                        onClickListener = { startClaimingFlow(DeviceType.PULSE_4G) },
                    ) {
                        TypeContent(stringResource(R.string.pulse_4g), R.drawable.device_type_pulse)
                    }
                }
            }
        }
    }

    @Suppress("FunctionNaming")
    @Composable
    fun TypeContent(name: String, imageResId: Int) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_normal)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            Image(painter = painterResource(imageResId), contentDescription = null)
            MediumText(text = name, fontWeight = FontWeight.Bold, colorRes = R.color.darkestBlue)
        }
    }
}
