package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.AgencyCardActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnAgencyCardScreen
import com.yandex.mobile.realty.core.robot.performOnNoteScreen
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.Screen
import com.yandex.mobile.realty.domain.model.ScreenReferrer
import com.yandex.mobile.realty.domain.model.agency.AgencyContext
import com.yandex.mobile.realty.domain.model.agency.AgencyProfilePreview
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.ui.model.AgencyCardParams
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.*

/**
 * @author misha-kozlov on 1/26/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AgencyCardNotesTest : NotesTest() {

    private val activityTestRule = AgencyCardActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldAddNote() {
        configureWebServer {
            registerAgencyCard()
            registerOffers()
            registerNoteSaving(OFFER_ID, TEXT)
        }

        launchAgencyActivity()
        performOnAgencyCardScreen {
            waitUntil { containsTotalOffersSubtitle() }
            collapseAppBar()

            waitUntil { containsOfferSnippet(OFFER_ID) }
            scrollByFloatingButtonHeight()

            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesMenuButton())
            }
        }
        performOnOfferMenuDialog {
            isAddNoteButtonShown()
            tapOn(lookup.matchesAddNoteButton())
        }
        performOnNoteScreen {
            typeText(lookup.matchesInputView(), TEXT)
            tapOn(lookup.matchesSubmitButton())
        }
        performOnAgencyCardScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
            scrollByFloatingButtonHeight()

            performOnOfferSnippet(OFFER_ID) {
                isNoteShown(TEXT)
            }
        }
    }

    private fun launchAgencyActivity() {
        val agencyContext = AgencyContext.Sell(
            regionId = 587_795,
            agencyPreview = AgencyProfilePreview(
                uid = "1",
                creationDate = Calendar.getInstance().run {
                    set(2020, 5, 22)
                    time
                },
                userType = AgencyProfilePreview.UserType.AGENCY,
                name = "Этажи",
                photo = "file:///sdcard/realty_images/test_image.jpeg"
            ),
            property = Filter.Property.APARTMENT
        )

        val params = AgencyCardParams(
            agencyContext = agencyContext,
            screenReferrer = ScreenReferrer.valueOf(Screen.OFFER_DETAILS)
        )

        activityTestRule.launchActivity(AgencyCardActivityTestRule.createIntent(params))
    }

    private fun DispatcherRegistry.registerAgencyCard() {
        register(
            request {
                path("2.0/agencies/active/user/uid:$UID")
            },
            response {
                assetBody("agencyTest/agency.json")
            }
        )
        register(
            request {
                path("1.0/dynamicBoundingBox")
            },
            response {
                assetBody("agencyTest/agencyAllOffersBoundingBox.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("notesTest/offerWithSiteSearch.json")
            }
        )
    }
}
