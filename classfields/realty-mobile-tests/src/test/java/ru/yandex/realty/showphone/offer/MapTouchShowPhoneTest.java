package ru.yandex.realty.showphone.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.mock.OfferPhonesResponse;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.KARTA;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.listing.TouchOffer.CALL_BUTTON;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferPhonesResponse.offersPhonesTemplate;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.mock.PointStatisticSearchTemplate.pointStatisticSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.UtilsWeb.getHrefForPhone;

@DisplayName("Показ телефона. Карта")
@Feature(MAP)
@Link("https://st.yandex-team.ru/VERTISTEST-1599")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class MapTouchShowPhoneTest {

    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";
    private static final int PIN = 0;

    private MockOffer offer;
    private OfferPhonesResponse offersPhonesTemplate;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        basePageSteps.resize(380, 950);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Позвонить» на карте")
    public void shouldSeePhoneNormalCaseMap() {
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(PIN));
        basePageSteps.onMobileMapPage().offer(FIRST).link(CALL_BUTTON).click();
        basePageSteps.onMobileMapPage().offer(FIRST).link(CALL_BUTTON)
                .should(hasHref(equalTo(getHrefForPhone(TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Позвонить». Показ двух телефонов")
    public void shouldSeeTwoPhoneMap() {
        String secondPhone = format("+%s", getRandomPhone());
        offer.addPhoneNumber(secondPhone);
        List<String> phoneList = asList(getHrefForPhone(TEST_PHONE), getHrefForPhone(SECOND_TEST_PHONE));
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE).addPhone(SECOND_TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(PIN));
        basePageSteps.onMobileMapPage().offer(FIRST).button(CALL_BUTTON).click();
        basePageSteps.onMobileMapPage().phones().should(hasSize(phoneList.size()));
        assertThat(basePageSteps.onMobileMapPage().phones().stream().map(p -> p.getAttribute("href"))
                .collect(Collectors.toList())).containsExactlyElementsOf(phoneList);
    }

}
