package ru.auto.ara.test.offer.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewsFeaturesDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.review.Feature

@RunWith(AndroidJUnit4::class)
class PlusMinus1SegmentTest {
    private val uri = "https://auto.ru/cars/used/sale/1084155311-742cfbff"
    private val offerId = "1084155311-742cfbff"
    private val category = "cars"
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer(category = category, offerId = offerId),
            GetReviewsFeaturesDispatcher(category = category, countOfSegments = "1_segment")
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

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
        activityTestRule.launchDeepLinkActivity(uri)
        performOfferCard { scrollToPlusMinusWithoutOverallRating() }
    }

    @Test
    fun shouldSeePlusMinusWith1Segment() {
        checkOfferCard {
            isPlusMinusExpandViewDisplayed("50 / 8")
            isPlusMinusSegmentNotDisplayed()
            isAllPlusMinusButtonDisplayed()
            isPlusMinusFeatureDisplayed(
                listOf(
                    Feature("Комфорт", 10, 3, ""),
                    Feature("Дизайн", 10, 0, ""),
                    Feature("Шумоизоляция", 10, 1, ""),
                    Feature("Управляемость", 9, 2, "")
                )
            )
        }
    }
}
