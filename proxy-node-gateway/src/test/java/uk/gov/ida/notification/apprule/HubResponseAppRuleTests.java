package uk.gov.ida.notification.apprule;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import uk.gov.ida.notification.apprule.base.GatewayAppRuleTestBase;
import uk.gov.ida.notification.apprule.rules.GatewayAppRule;
import uk.gov.ida.notification.apprule.rules.RedisTestRule;
import uk.gov.ida.notification.apprule.rules.TestEidasSamlResource;
import uk.gov.ida.notification.apprule.rules.TestTranslatorClientErrorResource;
import uk.gov.ida.notification.apprule.rules.TestTranslatorResource;
import uk.gov.ida.notification.apprule.rules.TestTranslatorServerErrorResource;
import uk.gov.ida.notification.apprule.rules.TestVerifyServiceProviderResource;
import uk.gov.ida.notification.contracts.HubResponseTranslatorRequest;
import uk.gov.ida.notification.helpers.HtmlHelpers;
import uk.gov.ida.notification.saml.SamlFormMessageType;
import uk.gov.ida.notification.shared.Urls;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_COUNTRY_PUBLIC_PRIMARY_CERT;

public class HubResponseAppRuleTests extends GatewayAppRuleTestBase {

    private static final int EMBEDDED_REDIS_PORT = 6380;

    private static final TestTranslatorResource testTranslatorResource = new TestTranslatorResource();

    @ClassRule
    public static final DropwizardClientRule translatorClientRule = new DropwizardClientRule(testTranslatorResource);

    @ClassRule
    public static final DropwizardClientRule translatorClientServerErrorRule = new DropwizardClientRule(new TestTranslatorServerErrorResource());

    @ClassRule
    public static final DropwizardClientRule translatorClientClientErrorRule = new DropwizardClientRule(new TestTranslatorClientErrorResource());

    @ClassRule
    public static final DropwizardClientRule espClientRule = new DropwizardClientRule(new TestEidasSamlResource());

    @ClassRule
    public static final DropwizardClientRule vspClientRule = new DropwizardClientRule(new TestVerifyServiceProviderResource());

    @ClassRule
    public static final RedisTestRule embeddedRedis = new RedisTestRule(EMBEDDED_REDIS_PORT);

    private final String redisMockURI = this.setupTestRedis();

    @Rule
    public GatewayAppRule proxyNodeAppRule = new GatewayAppRule(
        ConfigOverride.config("eidasSamlParserService.url", espClientRule.baseUri().toString()),
        ConfigOverride.config("verifyServiceProviderService.url", vspClientRule.baseUri().toString()),
        ConfigOverride.config("translatorService.url", translatorClientRule.baseUri().toString()),
        ConfigOverride.config("redisService.url", redisMockURI)
    );

    @Rule
    public GatewayAppRule proxyNodeAppRuleEmbeddedRedis = new GatewayAppRule(
        ConfigOverride.config("eidasSamlParserService.url", espClientRule.baseUri().toString()),
        ConfigOverride.config("verifyServiceProviderService.url", vspClientRule.baseUri().toString()),
        ConfigOverride.config("translatorService.url", translatorClientRule.baseUri().toString()),
        ConfigOverride.config("redisService.url", "redis://localhost:" + EMBEDDED_REDIS_PORT)
    );

    @Rule
    public GatewayAppRule proxyNodeServerErrorAppRule = new GatewayAppRule(
        ConfigOverride.config("eidasSamlParserService.url", espClientRule.baseUri().toString()),
        ConfigOverride.config("verifyServiceProviderService.url", vspClientRule.baseUri().toString()),
        ConfigOverride.config("translatorService.url", translatorClientServerErrorRule.baseUri().toString()),
        ConfigOverride.config("redisService.url", redisMockURI)
    );

    @Rule
    public GatewayAppRule proxyNodeClientErrorAppRule = new GatewayAppRule(
        ConfigOverride.config("eidasSamlParserService.url", espClientRule.baseUri().toString()),
        ConfigOverride.config("verifyServiceProviderService.url", vspClientRule.baseUri().toString()),
        ConfigOverride.config("translatorService.url", translatorClientClientErrorRule.baseUri().toString()),
        ConfigOverride.config("redisService.url", redisMockURI)
    );

    private final Form postForm = new Form()
        .param(SamlFormMessageType.SAML_RESPONSE, Base64.encodeAsString("I'm going to be a SAML blob"))
        .param("RelayState", "relay-state");

    @Test
    public void hubResponseReturnsHtmlFormWithSamlBlob() throws Exception {
        Response response = proxyNodeAppRule
            .target(Urls.GatewayUrls.GATEWAY_HUB_RESPONSE_RESOURCE)
            .request()
            .cookie(getSessionCookie(proxyNodeAppRule))
            .post(Entity.form(postForm));

        assertThat(response.getStatus()).isEqualTo(200);

        String htmlString = response.readEntity(String.class);
        HtmlHelpers.assertXPath(
            htmlString,
            String.format(
                "//form[@action='http://connector-node.com']/input[@name='SAMLResponse'][@value='%s']",
                TestTranslatorResource.SAML_BLOB
            )
        );
        HtmlHelpers.assertXPath(
            htmlString,
            "//form[@action='http://connector-node.com']/input[@name='RelayState'][@value='relay-state']"
        );
    }

    @Test
    public void redisCanStoreCertificateInSession() throws Throwable {
        Response response = proxyNodeAppRuleEmbeddedRedis
            .target(Urls.GatewayUrls.GATEWAY_HUB_RESPONSE_RESOURCE)
            .request()
            .cookie(getSessionCookie(proxyNodeAppRuleEmbeddedRedis))
            .post(Entity.form(postForm));

        assertThat(200).isEqualTo(response.getStatus());

        final List<HubResponseTranslatorRequest> translatorArgs = testTranslatorResource.getTranslatorArgs();
        assertThat(translatorArgs.get(0).getConnectorEncryptionCertificate()).isEqualTo(STUB_COUNTRY_PUBLIC_PRIMARY_CERT);
        assertThat(translatorArgs.size()).isOne();
    }

    @Test
    public void returnsErrorPageIfSessionMissingException() throws Exception {
        Response response = proxyNodeAppRule
            .target(Urls.GatewayUrls.GATEWAY_HUB_RESPONSE_RESOURCE)
            .request()
            .post(Entity.form(postForm));

        assertThat(response.getStatus()).isEqualTo(400);

        String htmlString = response.readEntity(String.class);
        HtmlHelpers.assertXPath(
            htmlString,
            "//div[@class='issues'][text()='Something went wrong; session does not exist']"
        );
    }

    @Test
    public void serverErrorResponseFromTranslatorReturns500() throws Exception {
        Response response = proxyNodeServerErrorAppRule
            .target(Urls.GatewayUrls.GATEWAY_HUB_RESPONSE_RESOURCE)
            .request()
            .cookie(getSessionCookie(proxyNodeServerErrorAppRule))
            .post(Entity.form(postForm));

        assertThat(response.getStatus()).isEqualTo(500);

        String htmlString = response.readEntity(String.class);
        HtmlHelpers.assertXPath(
            htmlString,
            "//div[@class='issues'][text()='Something went wrong when contacting the Translator']"
        );
    }

    @Test
    public void clientErrorResponseFromTranslatorReturns400() throws Exception {
        Response response = proxyNodeClientErrorAppRule
            .target(Urls.GatewayUrls.GATEWAY_HUB_RESPONSE_RESOURCE)
            .request()
            .cookie(getSessionCookie(proxyNodeClientErrorAppRule))
            .post(Entity.form(postForm));

        assertThat(response.getStatus()).isEqualTo(400);

        String htmlString = response.readEntity(String.class);
        HtmlHelpers.assertXPath(
            htmlString,
            "//div[@class='issues'][text()='Something went wrong when contacting the Translator']"
        );
    }

    private NewCookie getSessionCookie(GatewayAppRule appRule) throws Exception {
        return postEidasAuthnRequest(buildAuthnRequest(), appRule).getCookies().get("gateway-session");
    }

    @AfterAll
    public void tearDown() {
        this.killTestRedis();
    }
}
