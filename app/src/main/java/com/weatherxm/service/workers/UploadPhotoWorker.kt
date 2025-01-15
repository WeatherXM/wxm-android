package com.weatherxm.service.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.data.repository.DevicePhotoRepository
import com.weatherxm.data.requireNetwork
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class UploadPhotoWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    companion object {
        private const val UPLOAD_PHOTO_PREFIX = "UPLOAD_PHOTO_"
        private const val UPLOAD_URL = "uploadUrl"
        private const val PHOTO_PATH = "photoPath"
        private const val BUCKET = "bucket"
        private const val ALGO = "X-Amz-Algorithm"
        private const val CREDENTIALS = "X-Amz-Credential"
        private const val DATE = "X-Amz-Date"
        private const val SECURITY_TOKEN = "X-Amz-Security-Token"
        private const val SIGNATURE = "X-Amz-Signature"
        private const val KEY = "key"
        private const val POLICY = "Policy"

        fun cancelWorkers(context: Context, deviceId: String) {
            Timber.d("Cancelling Upload Photos Work Manager for device [$deviceId].")
            WorkManager.getInstance(context).cancelAllWorkByTag(UPLOAD_PHOTO_PREFIX + deviceId)
        }

        fun initAndStart(
            context: Context,
            metadata: PhotoPresignedMetadata,
            photoPath: String,
            deviceId: String
        ) {
            Timber.d("Creating Upload Photos Work Manager for device [$deviceId].")
            val data = Data.Builder()
                .putString(ARG_DEVICE_ID, deviceId)
                .putString(PHOTO_PATH, photoPath)
                .putString(UPLOAD_URL, metadata.url)
                .putString(BUCKET, metadata.fields.bucket)
                .putString(ALGO, metadata.fields.xAmzAlgo)
                .putString(CREDENTIALS, metadata.fields.xAmzCredentials)
                .putString(DATE, metadata.fields.xAmzDate)
                .putString(SECURITY_TOKEN, metadata.fields.xAmzSecurityToken)
                .putString(SIGNATURE, metadata.fields.xAmzSignature)
                .putString(KEY, metadata.fields.key)
                .putString(POLICY, metadata.fields.policy)

            val uploadRequest = OneTimeWorkRequestBuilder<UploadPhotoWorker>()
                .addTag(UPLOAD_PHOTO_PREFIX + deviceId)
                .setConstraints(Constraints.requireNetwork())
                .setInputData(data.build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$UPLOAD_PHOTO_PREFIX${photoPath}_$deviceId",
                ExistingWorkPolicy.REPLACE,
                uploadRequest
            )
        }
    }

    private val photoRepository: DevicePhotoRepository by inject()

    override suspend fun doWork(): Result {
        val deviceId = workerParams.inputData.getString(ARG_DEVICE_ID)
        val uploadUrl = workerParams.inputData.getString(UPLOAD_URL)
        val photoPath = workerParams.inputData.getString(PHOTO_PATH)
        val bucket = workerParams.inputData.getString(BUCKET)
        val algo = workerParams.inputData.getString(ALGO)
        val credentials = workerParams.inputData.getString(CREDENTIALS)
        val date = workerParams.inputData.getString(DATE)
        val securityToken = workerParams.inputData.getString(SECURITY_TOKEN)
        val signature = workerParams.inputData.getString(SIGNATURE)
        val key = workerParams.inputData.getString(KEY)
        val policy = workerParams.inputData.getString(POLICY)
        if (deviceId.isNullOrEmpty() || uploadUrl.isNullOrEmpty() || photoPath.isNullOrEmpty()) {
            Timber.d("Cancelling Upload Photos Work Manager for device [$deviceId].")
            WorkManager.getInstance(context).cancelWorkById(workerParams.id)
            return Result.failure()
        }
        Timber.d("Starting Upload Photos Work Manager for device [$deviceId].")

        with(MultipartUploadRequest(context, uploadUrl).setMethod("POST")) {
            bucket?.let { addParameter(BUCKET, bucket) }
            algo?.let { addParameter(ALGO, algo) }
            credentials?.let { addParameter(CREDENTIALS, credentials) }
            date?.let { addParameter(DATE, date) }
            securityToken?.let { addParameter(SECURITY_TOKEN, securityToken) }
            signature?.let { addParameter(SIGNATURE, signature) }
            key?.let { addParameter(KEY, key) }
            policy?.let { addParameter(POLICY, policy) }
            addFileToUpload(photoPath, "file")
            val uploadId = startUpload()
            photoRepository.addDevicePhotoUploadIdAndRequest(deviceId, uploadId, this)
        }

        return Result.success()
    }
}
