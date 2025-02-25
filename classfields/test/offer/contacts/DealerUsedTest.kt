package ru.auto.ara.test.offer.contacts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.actions.ViewActions.setAppBarExpandedState
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.salon.getCustomizableSalonById
import ru.auto.ara.core.robot.dealer.checkDealerContacts
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getAutoRuYears
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class DealerUsedTest {
    private val webServerRule = WebServerRule {
        getCustomizableSalonById(
            dealerId = 20867288,
            dealerCode = "avtomir_moskva_irkutskaya"
        )
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1090274296-54049309")
        )
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1090274296-54049309")
        performOfferCard {
            interactions.onAppBar().waitUntilIsCompletelyDisplayed().perform(setAppBarExpandedState(false))
            scrollToRequestCall()
        }
    }

    @Test
    fun shouldSeeDealerContactsBlock() {
        checkOfferCard {
            isDealerContactsDisplayed(
                name = "Автомир Байкальская",
                officialLabel = "Дилер",
                ageLabel = "На Авто.ру ${getAutoRuYears(2013)} лет",
                loyal = true,
                isOfficialDealer = false
            )
        }
    }

    @Test
    fun shouldOpenDealersFeedAfterClickDealerName() {
        performOfferCard {
            scrollToRequestCall()
            interactions.onDealerName().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkDealerContacts { checkDealerContactsDialogIsDisplayed() }
    }
}
