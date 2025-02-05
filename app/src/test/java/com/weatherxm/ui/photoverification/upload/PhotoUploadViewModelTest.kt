package com.weatherxm.ui.photoverification.upload

import android.net.Uri
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.util.ImageFileHelper
import com.weatherxm.util.ImageFileHelper.getUriForFile
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File

class PhotoUploadViewModelTest : BehaviorSpec({
    val device = UIDevice.empty()
    val usecase = mockk<DevicePhotoUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val viewModel =
        PhotoUploadViewModel(device, mutableListOf(), usecase, resources, analytics, dispatcher)

    val localPath = "localPath"
    val localPhoto = StationPhoto(null, localPath)
    val remotePhoto = StationPhoto("remotePath", null)
    val localPhotosPaths = listOf(localPhoto.localPath!!)
    val uri = mockk<Uri>()
    val listOfUrisOfLocalPhotos = arrayListOf(uri)
    val metadata = listOf<PhotoPresignedMetadata>()

    val deviceNotFoundFailure = ApiError.DeviceNotFound("")
    val unauthorizedFailure = ApiError.GenericError.JWTError.UnauthorizedError("", "unauthorized")

    listener(InstantExecutorListener())

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<Resources> {
                        resources
                    }
                }
            )
        }
        mockkObject(ImageFileHelper)
        every { File(localPath).getUriForFile(context) } returns uri
        justRun { analytics.trackEventFailure(any()) }
    }

    suspend fun BehaviorSpecWhenContainerScope.testPrepareUploadFailure(
        verifyNumberOfFailureEvents: Int,
        errorMsg: String?
    ) {
        testHandleFailureViewModel(
            { viewModel.prepareUpload() },
            analytics,
            viewModel.onPhotosPresignedMetadata(),
            verifyNumberOfFailureEvents,
            errorMsg ?: String.empty()
        )
    }

    context("Get the URIs of the local photos") {
        When("There are no photos with local paths") {
            then("return an empty arraylist") {
                viewModel.getUrisOfLocalPhotos(context) shouldBe arrayListOf()
            }
        }
        When("There are some photos with local path") {
            viewModel.photos.add(localPhoto)
            then("return the arraylist containing the URIs of those photos") {
                viewModel.getUrisOfLocalPhotos(context) shouldBe listOfUrisOfLocalPhotos
            }
        }
    }

    context("Prepare upload (i.e. get the photos metadata needed)") {
        given("a usecase providing the response containing the metadata") {
            viewModel.photos.add(remotePhoto)
            When("it is a failure") {
                and("It's a DeviceNotFound failure") {
                    coMockEitherLeft(
                        { usecase.getPhotosMetadataForUpload(device.id, localPhotosPaths) },
                        deviceNotFoundFailure
                    )
                    testPrepareUploadFailure(1, DEVICE_NOT_FOUND_MSG)
                }
                and("It's an UnauthorizedFailure failure") {
                    coMockEitherLeft(
                        { usecase.getPhotosMetadataForUpload(device.id, localPhotosPaths) },
                        unauthorizedFailure
                    )
                    testPrepareUploadFailure(2, unauthorizedFailure.message)
                }
                and("it's any other failure") {
                    coMockEitherLeft(
                        { usecase.getPhotosMetadataForUpload(device.id, localPhotosPaths) },
                        failure
                    )
                    testPrepareUploadFailure(3, REACH_OUT_MSG)
                }
            }
            When("it is a success") {
                coMockEitherRight(
                    { usecase.getPhotosMetadataForUpload(device.id, localPhotosPaths) },
                    metadata
                )
                runTest { viewModel.prepareUpload() }

                then("LiveData at onPhotosPresignedMetadata posts a success with the metadata") {
                    viewModel.onPhotosPresignedMetadata().isSuccess(metadata)
                }
            }
        }
    }

    afterSpec {
        clearMocks(ImageFileHelper)
        stopKoin()
    }
})
