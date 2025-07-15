package com.weatherxm.ui.claimdevice.photosgallery

import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.PhotoSource
import com.weatherxm.ui.common.StationPhoto
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ClaimPhotosGalleryViewModelTest : BehaviorSpec({
    val emptyPhotos = mutableListOf<StationPhoto>()
    val viewModel = ClaimPhotosGalleryViewModel()

    val localPath = "localPath"
    val localPhoto = StationPhoto(null, localPath, PhotoSource.GALLERY)
    val photosListWithOneLocalPhoto = mutableListOf(
        StationPhoto(
            null,
            localPath,
            PhotoSource.GALLERY
        )
    )

    listener(InstantExecutorListener())

    context("Add a photo") {
        given("the path of the photo") {
            When("the path is empty") {
                then("Do nothing. So the photos should still be the same empty list.") {
                    viewModel.addPhoto("", PhotoSource.GALLERY)
                    viewModel.onPhotos shouldBe emptyPhotos
                }
            }
            When("the path is not empty") {
                viewModel.addPhoto(localPath, PhotoSource.GALLERY)
                then("the photo should be added") {
                    viewModel.onPhotos shouldBe photosListWithOneLocalPhoto
                }
            }
        }
    }

    context("Delete a photo") {
        given("the photo to delete") {
            When("we do not have this photo in our list") {
                viewModel.deletePhoto(StationPhoto(null, null))
                then("Do nothing. So the photos should still be the same list.") {
                    viewModel.onPhotos shouldBe photosListWithOneLocalPhoto
                }
            }
            When("the photo is a local photo and our photos var contain it") {
                viewModel.deletePhoto(localPhoto)
                then("The onPhotos function should return the empty list") {
                    viewModel.onPhotos shouldBe emptyPhotos
                }
            }
        }
    }

    context("Request camera permissions") {
        given("a function to trigger that request") {
            then("the respective LiveData initially should be false") {
                viewModel.onRequestCameraPermission().value shouldBe false
            }
            viewModel.requestCameraPermission()
            then("the respective LiveData should be triggered") {
                viewModel.onRequestCameraPermission().value shouldBe true
            }
        }
    }
})
