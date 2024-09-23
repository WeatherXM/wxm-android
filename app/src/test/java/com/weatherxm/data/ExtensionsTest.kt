package com.weatherxm.data

import androidx.work.Constraints
import androidx.work.NetworkType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response

class ExtensionsTest : BehaviorSpec({
    val path = "/api/v1/me/devices"
    val request = Request.Builder().url("https://api.weatherxm.com$path").build()
    val response =
        Response.Builder().request(request).code(200).protocol(Protocol.HTTP_2).message("").build()

    context("Get the path of an HTTP Request and Response") {
        given("An HTTP request") {
            then("Get its path") {
                request.path() shouldBe path
            }
        }

        given("An HTTP response") {
            then("Get its path") {
                response.path() shouldBe path
            }
        }
    }

    context("Create Constraints with NetworkType = CONNECTED") {
        given("An extension helper function we have created") {
            then("Use it to create the constraints") {
                Constraints.Companion
                    .requireNetwork()
                    .requiredNetworkType shouldBe NetworkType.CONNECTED
            }
        }
    }


})
