package com.weatherxm.util

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.search.result.SearchSuggestion
import com.squareup.moshi.Moshi
import com.weatherxm.R
import com.weatherxm.data.models.Hex
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.PublicHex
import com.weatherxm.ui.home.explorer.ExplorerViewModel.Companion.FILL_OPACITY_HEXAGONS
import com.weatherxm.ui.home.explorer.MapLayer
import com.weatherxm.ui.home.explorer.UICell
import com.weatherxm.ui.home.explorer.UICellJsonAdapter
import com.weatherxm.util.MapboxUtils.getCustomData
import com.weatherxm.util.MapboxUtils.parseSearchSuggestion
import com.weatherxm.util.MapboxUtils.polygonPointsToLatLng
import com.weatherxm.util.MapboxUtils.toPolygonAnnotationOptions
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class MapboxUtilsTest : BehaviorSpec({
    lateinit var gson: Gson
    val resources = mockk<android.content.res.Resources>()
    val searchSuggestion = mockk<SearchSuggestion>()
    val polygonAnnotation = mockk<PolygonAnnotation>()
    val address = "Address"
    val street = "Street"
    val place = "Place"
    val region = "Region"
    val country = "Country"
    val postcode = "Postcode"
    val minimapWidth = 500
    val userLocation = Location.empty()
    val userHex = Hex(
        "cellIndex",
        arrayOf(
            Location(lat = 38.72482636074315, lon = 27.214679947948405),
            Location(lat = 38.71114716956354, lon = 27.21391178034676),
            Location(lat = 38.70374631403231, lon = 27.22820018964633),
            Location(lat = 38.71002341064811, lon = 27.24325838250108),
            Location(lat = 38.72370239640629, lon = 27.24403062984063),
            Location(lat = 38.73110449138368, lon = 27.22974060478422)
        ),
        Location(lat = 38.71742582818318, lon = 27.22897028978693)
    )
    val correctMinimapUrl = "https://api.mapbox.com/styles/v1/mapbox/dark-v10/static/" +
        "pin-m+0A3FAD(0.000000,0.000000)," +
        "path-0.0+FFFFFF+3388ff-0.5(emjkFwjbeDntAxCfm@ixAef@c%7DAotAyCgm@hxA)" +
        "/27.228970,38.717426,11.000000,0.000000,0.000000" +
        "/500x200@2x?access_token=pk.TEST"
    val publicHex = PublicHex(
        "cellIndex",
        0,
        0,
        0,
        0,
        Location(lat = 38.71742582818318, lon = 27.22897028978693),
        listOf(
            Location(lat = 38.72482636074315, lon = 27.214679947948405),
            Location(lat = 38.71114716956354, lon = 27.21391178034676),
            Location(lat = 38.70374631403231, lon = 27.22820018964633),
            Location(lat = 38.71002341064811, lon = 27.24325838250108),
            Location(lat = 38.72370239640629, lon = 27.24403062984063),
            Location(lat = 38.73110449138368, lon = 27.22974060478422)
        )
    )

    beforeSpec {
        startKoin {
            modules(
                module {
                    val moshi = Moshi.Builder().build()
                    single<UICellJsonAdapter> {
                        UICellJsonAdapter(moshi)
                    }
                    single<Resources> {
                        Resources(resources)
                    }
                    single<Gson> {
                        GsonBuilder().setPrettyPrinting()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create()
                    }
                }
            )
        }

        gson = GsonBuilder().setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        every {
            polygonAnnotation.getJsonObjectCopy().getAsJsonObject("custom_data")
        } returns gson.toJsonTree(UICell("cellIndex", Location.empty())).asJsonObject

        every { searchSuggestion.name } returns address
        every { searchSuggestion.address } returns mockk()
        every { searchSuggestion.address?.street } returns null
        every { searchSuggestion.address?.place } returns null
        every { searchSuggestion.address?.region } returns null
        every { searchSuggestion.address?.country } returns null
        every { searchSuggestion.address?.postcode } returns null
        every { resources.getString(R.string.mapbox_access_token) } returns "pk.TEST"
        every { resources.getColor(R.color.error, null) } returns 0
        every { resources.getColor(R.color.white, null) } returns 0
    }

    context("Parse PolygonAnnotation to UICell UI Model") {
        given("A PolygonAnnotation") {
            then("return the UICell UI Model") {
                getCustomData(polygonAnnotation) shouldBe UICell("cellIndex", Location.empty())
            }
        }
    }

    context("Parse a SearchSuggestion to a human-friendly string") {
        given("A SearchSuggestion") {
            When("Only the name is available") {
                then("return the name") {
                    parseSearchSuggestion(searchSuggestion) shouldBe address
                }
            }
            When("Only the name and street are available") {
                every { searchSuggestion.address?.street } returns street
                then("return the name and street") {
                    parseSearchSuggestion(searchSuggestion) shouldBe "$address, $street"
                }
            }
            When("Only the name, street and place are available") {
                every { searchSuggestion.address?.place } returns place
                then("return the name, street and place") {
                    parseSearchSuggestion(searchSuggestion) shouldBe "$address, $street, $place"
                }
            }
            When("Only the name, street, place and region are available") {
                every { searchSuggestion.address?.region } returns region
                then("return the name, street, place and region") {
                    parseSearchSuggestion(searchSuggestion) shouldBe
                        "$address, $street, $place, $region"
                }
            }
            When("Only the name, street, place, region and country are available") {
                every { searchSuggestion.address?.country } returns country
                then("return the name, street, place, region and country") {
                    parseSearchSuggestion(searchSuggestion) shouldBe
                        "$address, $street, $place, $region, $country"
                }
            }
            When("Only the name, street, place, region, country and postcode are available") {
                every { searchSuggestion.address?.postcode } returns postcode
                then("return the name, street, place, region, country and postcode") {
                    parseSearchSuggestion(searchSuggestion) shouldBe
                        "$address, $street, $place, $region, $country, $postcode"
                }
            }
        }
    }

    context("Parse a list of public hexes to a list of PolygonAnnotationOptions") {
        given("A list of Public Hexes") {
            When("The list is empty") {
                then("return an empty list") {
                    listOf<PublicHex>()
                        .toPolygonAnnotationOptions(MapLayer.DATA_QUALITY) shouldBe emptyList()
                }
            }
            When("The list is not empty") {
                then("return the respective PolygonAnnotation list") {
                    val expectedResult = listOf(
                        PolygonAnnotationOptions()
                            .withFillColor(resources.getColor(R.color.error, null))
                            .withFillOpacity(FILL_OPACITY_HEXAGONS)
                            .withFillOutlineColor(resources.getColor(R.color.white, null))
                            .withData(gson.toJsonTree(UICell(publicHex.index, publicHex.center)))
                            .withPoints(listOf(polygonPointsToLatLng(publicHex.polygon)))
                    )

                    val returnedList =
                        listOf(publicHex).toPolygonAnnotationOptions(MapLayer.DATA_QUALITY)
                    returnedList[0].fillColor shouldBe expectedResult[0].fillColor
                    returnedList[0].fillOpacity shouldBe expectedResult[0].fillOpacity
                    returnedList[0].fillOutlineColor shouldBe expectedResult[0].fillOutlineColor
                    returnedList[0].getData() shouldBe expectedResult[0].getData()
                    returnedList[0].getPoints() shouldBe expectedResult[0].getPoints()
                }
            }
        }
    }

    context("Build a minimap URL") {
        given("A User Width, User Location and the Hex") {
            When("The Hex is null") {
                then("return null") {
                    MapboxUtils.getMinimap(minimapWidth, userLocation, null) shouldBe null
                }
            }
            When("The Hex is not null") {
                then("return a URL") {
                    val minimapUrl = MapboxUtils.getMinimap(minimapWidth, userLocation, userHex)
                        ?.toUrl()
                        .toString()
                    minimapUrl shouldBe correctMinimapUrl
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
