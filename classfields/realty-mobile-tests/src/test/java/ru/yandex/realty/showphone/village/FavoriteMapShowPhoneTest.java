package ru.yandex.realty.showphone.village;

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
import ru.yandex.realty.mock.MockVillage;
import ru.yandex.realty.mock.NewbuildingContactResponse;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.consts.Pages.KARTA;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;
import static ru.yandex.realty.element.offercard.PhoneBlock.TEL_HREF_PATTERN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.map.MapOffer.CALL;
import static ru.yandex.realty.mock.FavoritesResponse.favoritesTemplate;
import static ru.yandex.realty.mock.MockVillage.VILLAGE_COTTAGE;
import static ru.yandex.realty.mock.MockVillage.mockVillage;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.VillagePointSearchTemplate.villagePointSearchTemplate;
import static ru.yandex.realty.mock.VillageSearchResponse.villageSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Показ телефона. Избранное карта")
@Feature(VILLAGE_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class FavoriteMapShowPhoneTest {

    public static final String ID = "200200";
    private static final String TEST_PHONE = "+71112223344";

    private NewbuildingContactResponse newbuildingContactResponse;
    private String favoritesResponse;
    private MockVillage village;

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
        village = mockVillage(VILLAGE_COTTAGE).setId(ID);
        favoritesResponse = favoritesTemplate().addItem(format("village_%s", ID)).build();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона избранное карта. Бесплатный поселок")
    public void shouldSeePhoneFreeVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villagePointSearch(villagePointSearchTemplate().setId(ID).build())
                .villageSearchStub(villageSearchTemplate().villages(asList(village)).build())
                .favoritesStub(favoritesResponse)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().offer(FIRST).link(CALL));
        basePageSteps.onMobileMapPage().offer(FIRST).link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона избранное карта. Платный поселок")
    public void shouldSeePhonePayedVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villagePointSearch(villagePointSearchTemplate().setId(ID).build())
                .villageSearchStub(villageSearchTemplate().villages(asList(village)).build())
                .favoritesStub(favoritesResponse)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().offer(FIRST).link(CALL));
        basePageSteps.onMobileMapPage().offer(FIRST).link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки избранное")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .villagePointSearch(villagePointSearchTemplate().setId(ID).build())
                .villageSearchStub(villageSearchTemplate().villages(asList(village)).build())
                .favoritesStub(favoritesResponse)
                .villageContactsStub500(ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));

        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().offer(FIRST).link(CALL));
        basePageSteps.acceptAlert();
        basePageSteps.onMobileMapPage().offer(FIRST).link(CALL).should(isDisplayed());
    }
}
