package com.weatherxm.ui.photoverification.gallery

import android.net.Uri
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.util.ImageFileHelper
import com.weatherxm.util.ImageFileHelper.getUriForFile
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.io.File

class PhotoGalleryViewModelTest : BehaviorSpec({
    val device = UIDevice.empty()
    val emptyPhotos = mutableListOf<StationPhoto>()
    val viewModel = PhotoGalleryViewModel(device, mutableListOf(), false, dispatcher)

    val localPath = "localPath"
    val localPhoto = StationPhoto(null, localPath)
    val remotePhoto = StationPhoto("remotePath", null)
    val photosListWithOneLocalPhoto = mutableListOf(StationPhoto(null, localPath))
    val uri = mockk<Uri>()
    val listOfUrisOfLocalPhotos = arrayListOf(uri)

    listener(InstantExecutorListener())

    beforeSpec {
        mockkObject(ImageFileHelper)
        every { File(localPath).getUriForFile(context) } returns uri
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
                    viewModel.addPhoto("")
                    viewModel.onPhotos shouldBe emptyPhotos
                }
            }
            When("the path is not empty") {
                and("the list of the photos doesn't contain that photo") {
                    viewModel.addPhoto(localPath)
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
                        viewModel.addPhoto("")
                        viewModel.photos shouldBe photosListWithOneLocalPhoto
                        viewModel.onPhotos shouldBe photosListWithOneLocalPhoto
                        viewModel.onPhotosNumber().value shouldBe photosListWithOneLocalPhoto.size
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
                viewModel.deletePhoto(remotePhoto)
                // TODO: STOPSHIP: Implement the delete endpoint's test
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
