package com.weatherxm.ui.claimdevice

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import arrow.core.Either
import arrow.core.rightIfNotNull
import com.weatherxm.data.ApiError.UserError.ClaimError
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.getParcelableExtra
import timber.log.Timber

fun getClaimResultHandler(
    activity: FragmentActivity,
    callback: (Either<Failure, Device>) -> Unit
) = activity.activityResultRegistry.register(
    Contracts.REQUEST_DEVICE_CLAIM,
    ActivityResultContracts.StartActivityForResult()
) { result ->
    when (result.resultCode) {
        Activity.RESULT_OK -> {
            result.data?.getParcelableExtra(Contracts.ARG_DEVICE, null)
                .rightIfNotNull {
                    ClaimError.UnknownClaimedDevice()
                }
                .tapLeft {
                    Timber.d("Claim error")
                    callback(
                        Either.Left(it)
                    )
                }
                .tap {
                    Timber.d("Claim success")
                    callback(
                        Either.Right(it as Device)
                    )
                }
        }
        Activity.RESULT_CANCELED -> {
            Timber.d("Claim cancelled")
            callback(
                Either.Left(ClaimError.ClaimCancelledError())
            )
        }
        else -> {
            Timber.d("Claim unknown result")
            callback(
                Either.Left(ClaimError.ClaimUnknownError())
            )
        }
    }
}
