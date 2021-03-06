package uk.gov.ida.notification.saml.deprecate;

import org.opensaml.saml.saml2.core.Status;

import java.util.Optional;

public abstract class SamlStatusToAuthenticationStatusCodeMapper<T extends Enum> {

    public abstract Optional<T> map(Status samlStatus);

    protected String getStatusCodeValue(final Status status) {
        return status.getStatusCode().getValue();
    }
}
