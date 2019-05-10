package uk.gov.ida.notification.exceptions.mappers.samlresponse;

import org.joda.time.DateTime;
import uk.gov.ida.notification.SamlFormViewBuilder;
import uk.gov.ida.notification.contracts.SamlFailureResponseGenerationRequest;
import uk.gov.ida.notification.exceptions.FailureResponseGenerationException;
import uk.gov.ida.notification.exceptions.ProducesSamlResponseException;
import uk.gov.ida.notification.proxy.TranslatorProxy;
import uk.gov.ida.notification.session.GatewaySessionData;
import uk.gov.ida.notification.session.storage.SessionStore;
import uk.gov.ida.notification.shared.ProxyNodeLogger;
import uk.gov.ida.notification.views.SamlFormView;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Optional;
import java.util.logging.Level;

import static java.text.MessageFormat.format;

public class ExceptionToSamlErrorResponseMapper implements ExceptionMapper<ProducesSamlResponseException> {

    private final ProxyNodeLogger proxyNodeLogger = new ProxyNodeLogger();
    private static final String SUBMIT_TEXT = "Continue";

    private final SamlFormViewBuilder samlFormViewBuilder;
    private final TranslatorProxy translatorProxy;
    private final SessionStore sessionStorage;

    private HttpServletRequest httpServletRequest;
    private UriInfo uriInfo;

    public ExceptionToSamlErrorResponseMapper(SamlFormViewBuilder samlFormViewBuilder, TranslatorProxy translatorProxy, SessionStore sessionStorage) {
        this.samlFormViewBuilder = samlFormViewBuilder;
        this.translatorProxy = translatorProxy;
        this.sessionStorage = sessionStorage;
    }

    @Context
    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Context
    public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public Response toResponse(ProducesSamlResponseException exception) {
        logException(exception);

        final String sessionId = httpServletRequest.getSession().getId();
        final GatewaySessionData sessionData = getSessionData(sessionId);

        final String samlErrorResponse = translatorProxy.getSamlErrorResponse(
                new SamlFailureResponseGenerationRequest(
                        getResponseStatus(exception),
                        sessionData.getEidasRequestId(),
                        sessionData.getEidasDestination()
                ));

        final SamlFormView samlFormView = samlFormViewBuilder.buildResponse(
                sessionData.getEidasDestination(),
                samlErrorResponse,
                SUBMIT_TEXT,
                sessionData.getEidasRelayState());

        return Response.ok().entity(samlFormView).build();
    }

    protected Response.Status getResponseStatus(ProducesSamlResponseException exception) {
        return exception.getResponseStatus();
    }

    private void logException(ProducesSamlResponseException exception) {
        final String message = exception.getMessage();
        final String cause = Optional.ofNullable(exception.getCause()).map(Throwable::getMessage).orElse(null);

        proxyNodeLogger.addContext(exception);

        proxyNodeLogger.log(Level.WARNING, format("Error whilst contacting uri [{0}]", uriInfo.getPath()));
    }

    private GatewaySessionData getSessionData(String sessionId) {
        try {
            return sessionStorage.getSession(sessionId);
        } catch (Throwable e) {
            throw new FailureResponseGenerationException(e);
        }
    }
}
