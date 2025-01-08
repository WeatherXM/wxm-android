package com.weatherxm.ui.photoverification.upload

import android.net.Uri
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.util.ImageFileHelper
import com.weatherxm.util.ImageFileHelper.getUriForFile
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.io.File

class PhotoUploadViewModelTest : BehaviorSpec({
    val device = UIDevice.empty()
    val usecase = mockk<DevicePhotoUseCase>()
    val viewModel = PhotoUploadViewModel(device, mutableListOf(), usecase, dispatcher)

    val localPath = "localPath"
    val localPhoto = StationPhoto(null, localPath)
    val uri = mockk<Uri>()
    val listOfUrisOfLocalPhotos = arrayListOf(uri)

    listener(InstantExecutorListener())

    beforeSpec {
        mockkObject(ImageFileHelper)
        every { File(localPath).getUriForFile(context) } returns uri
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

    afterSpec {
        clearMocks(ImageFileHelper)
    }
})
