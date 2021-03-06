package uk.gov.ida.notification.saml.validation;

import org.junit.Test;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.notification.VerifySamlInitializer;
import uk.gov.ida.notification.contracts.EidasSamlParserRequest;
import uk.gov.ida.notification.validations.AbstractDtoValidationsTest;

import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.notification.helpers.ValidationTestDataUtils.SAMPLE_HUB_SAML_AUTHN_REQUEST;

public class EidasSamlParserRequestValidationTests extends AbstractDtoValidationsTest<EidasSamlParserRequest> {

    static {
        try {
            InitializationService.initialize();
            VerifySamlInitializer.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldFailValidationWithNullRequest() {
        EidasSamlParserRequest nullRequest = new EidasSamlParserRequest(null);

        Map<String, List<ConstraintViolation<EidasSamlParserRequest>>> nullViolationsMap = validateAndMap(nullRequest);

        // the EidasSamlParserRequest request parameter is not marked @NotNull, this should pass
        assertThat(nullViolationsMap.size()).isEqualTo(1);
    }

    @Test
    public void shouldPassValidationWithValidRequest() {
        EidasSamlParserRequest goodRequest = new EidasSamlParserRequest(SAMPLE_HUB_SAML_AUTHN_REQUEST);

        Map<String, List<ConstraintViolation<EidasSamlParserRequest>>> goodViolationsMap = validateAndMap(goodRequest);

        assertThat(goodViolationsMap.size()).isEqualTo(0);
    }

    @Test
    public void shouldFailValidationWithBlankRequest() {
        EidasSamlParserRequest badRequest = new EidasSamlParserRequest("");

        Map<String,List<ConstraintViolation<EidasSamlParserRequest>>> badViolationsMap = validateAndMap(badRequest);

        assertThat(badViolationsMap.size()).isEqualTo(1);
    }
}
