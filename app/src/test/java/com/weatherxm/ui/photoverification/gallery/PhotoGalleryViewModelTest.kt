package com.weatherxm.ui.photoverification.gallery

import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class PhotoGalleryViewModelTest : BehaviorSpec({
    val device = UIDevice.empty()
    val emptyPhotos = mutableListOf<StationPhoto>()
    val usecase = mockk<DevicePhotoUseCase>()
    val viewModel = PhotoGalleryViewModel(device, mutableListOf(), false, usecase, dispatcher)

    val localPath = "localPath"
    val localPhoto = StationPhoto(null, localPath)
    val remotePhoto = StationPhoto("remotePath", null)
    val photosListWithOneLocalPhoto = mutableListOf(StationPhoto(null, localPath))

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
    }

    context("Get the initial states of the photos") {
        given("the photos passed as argument in the constructor of the view model") {
            then("onPhotos variable should have these photos") {
                viewModel.onPhotos shouldBe emptyPhotos
            }
            then("LiveData onPhotosNumber should post the size of that list of the photos") {
                viewModel.onPhotosNumber().value shouldBe emptyPhotos.size
            }
        }
    }

    context("Add a photo") {
        given("the path of the photo") {
            When("the path is empty") {
                then("Do nothing. So the photos should still be the same empty list.") {
                    viewModel.addPhoto("", null)
                    viewModel.onPhotos shouldBe emptyPhotos
                }
            }
            When("the path is not empty") {
                and("the list of the photos doesn't contain that photo") {
                    viewModel.addPhoto(localPath, null)
                    then("Add the photo in the constructor variable") {
                        viewModel.photos shouldBe photosListWithOneLocalPhoto
                    }
                    then("The onPhotos function should return that list with the one photo") {
                        viewModel.onPhotos shouldBe photosListWithOneLocalPhoto
                    }
                    then("LiveData onPhotosNumber should post the size of that one-photo list") {
                        viewModel.onPhotosNumber().value shouldBe photosListWithOneLocalPhoto.size
                    }
                }
                and("the list of the photos already contains that photo") {
                    then("Do nothing. So the photos should still be the same list.") {
                        viewModel.addPhoto("", null)
                        viewModel.photos shouldBe photosListWithOneLocalPhoto
                        viewModel.onPhotos shouldBe photosListWithOneLocalPhoto
                        viewModel.onPhotosNumber().value shouldBe photosListWithOneLocalPhoto.size
                    }
                }
                and("the number of local photos should be one") {
                    then("getLocalPhotosNumber should return 1") {
                        viewModel.getLocalPhotosNumber() shouldBe photosListWithOneLocalPhoto.size
                    }
                }
            }
        }
    }

    context("Delete a photo") {
        given("the photo to delete") {
            When("we do not have this photo in our list") {
                viewModel.deletePhoto(StationPhoto(null, null))
                then("Do nothing. So the photos should still be the same list.") {
                    viewModel.photos shouldBe photosListWithOneLocalPhoto
                    viewModel.onPhotos shouldBe photosListWithOneLocalPhoto
                    viewModel.onPhotosNumber().value shouldBe photosListWithOneLocalPhoto.size
                }
            }
            When("the photo is a local photo and our photos var contain it") {
                viewModel.deletePhoto(localPhoto)
                then("delete the photo in the constructor variable") {
                    viewModel.photos shouldBe emptyPhotos
                }
                then("The onPhotos function should return the empty list") {
                    viewModel.onPhotos shouldBe emptyPhotos
                }
                then("LiveData onPhotosNumber should post the size of an empty list") {
                    viewModel.onPhotosNumber().value shouldBe emptyPhotos.size
                }
            }
            When("the photo is a remote photo  and our photos var contain it") {
                viewModel.photos.add(remotePhoto)
                and("it's a failure") {
                    coMockEitherLeft(
                        { usecase.deleteDevicePhoto(device.id, "remotePath") },
                        failure
                    )
                    runTest { viewModel.deletePhoto(remotePhoto) }
                    then("LiveData posts an error with a specific error message") {
                        viewModel.onDeletingPhotoStatus().isError(REACH_OUT_MSG)
                    }
                }
                and("it's a success") {
                    coMockEitherRight(
                        { usecase.deleteDevicePhoto(device.id, "remotePath") },
                        Unit
                    )
                    runTest { viewModel.deletePhoto(remotePhoto) }
                    then("LiveData posts a success") {
                        viewModel.onDeletingPhotoStatus().isSuccess(Unit)
                    }
                    then("delete the photo in the constructor variable") {
                        viewModel.photos shouldBe emptyPhotos
                    }
                    then("The onPhotos function should return the empty list") {
                        viewModel.onPhotos shouldBe emptyPhotos
                    }
                    then("LiveData onPhotosNumber should post the size of an empty list") {
                        viewModel.onPhotosNumber().value shouldBe emptyPhotos.size
                    }
                }
            }
        }
    }

    context("Get the list of the local photos paths") {
        When("There are no photos with local paths") {
            then("return an empty arraylist") {
                viewModel.getPhotosLocalPaths() shouldBe arrayListOf()
            }
        }
        When("There are some photos with local path") {
            viewModel.photos.add(localPhoto)
            then("return the arraylist containing the paths as Strings of those photos") {
                viewModel.getPhotosLocalPaths() shouldBe arrayListOf(localPhoto.localPath)
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
