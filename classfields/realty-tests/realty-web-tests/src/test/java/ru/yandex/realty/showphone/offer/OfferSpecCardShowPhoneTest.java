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
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.mock.OfferPhonesResponse;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.element.offercard.PhoneBlock.MESSAGE_500;
import static ru.yandex.realty.element.offercard.PhoneBlock.TEL_HREF_PATTERN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_NEW_FLAT_SPEC_PROJECT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferPhonesResponse.offersPhonesTemplate;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@DisplayName("Показ телефона. Карточка оффера. Спецпроект")
@Feature(OFFER_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1599")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class OfferSpecCardShowPhoneTest {

    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";

    private MockOffer offer;
    private OfferPhonesResponse offersPhonesTemplate;
    private String offerId;

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
        offer = mockOffer(SELL_NEW_FLAT_SPEC_PROJECT);
        offerId = offer.getOfferId();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в микрокарточке. Спецпроект")
    public void shouldSeePhoneNormalCaseMicrocardSpec() {
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.onOfferCardPage().offerCardSummary().showPhoneButton().click();
        basePageSteps.onOfferCardPage().offerCardSummary().phones().should(hasSize(1)).get(0)
                .should(allOf(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_BRACKETS)),
                        hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE)))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При 500-ке показ заглушки клик в плавающей шапке. Спецпроект")
    public void shouldSeePhone500HideableBlockSpec() {
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerPhonesStub500(offer.getOfferId())
                .createWithDefaults();

        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.scrollDown(1000);
        basePageSteps.onOfferCardPage().hideableBlock().showPhoneButton().click();
        basePageSteps.onOfferCardPage().hideableBlock().showPhoneButton().should(hasText(containsString(MESSAGE_500)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ двух телефонов в галерее. Спецпроект")
    public void shouldSeeTwoPhoneGallerySpec() {
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE).addPhone(SECOND_TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.onOfferCardPage().openGallery();
        basePageSteps.onOfferCardPage().fsGallery().showPhoneButton().click();
        basePageSteps.onOfferCardPage().fsGallery().phones().get(0)
                .should(allOf(hasText(containsString(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_BRACKETS))),
                        hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE)))));
        basePageSteps.onOfferCardPage().fsGallery().phones().get(1)
                .should(allOf(hasText(makePhoneFormatted(SECOND_TEST_PHONE, PHONE_PATTERN_BRACKETS)),
                        hasHref(equalTo(format(TEL_HREF_PATTERN, SECOND_TEST_PHONE)))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в микрокарточке открывает другие телефоны. Спецпроект")
    public void shouldNotSeeOtherPhonesWhenRevealMicrocardSpec() {
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(OFFER).path(offerId).open();
        basePageSteps.onOfferCardPage().offerCardSummary().showPhoneButton().click();
        basePageSteps.scrollDown(1000);
        basePageSteps.onOfferCardPage().authorBlock().phone().should(isDisplayed());
        basePageSteps.onOfferCardPage().hideableBlock().phone().should(isDisplayed());
    }
}