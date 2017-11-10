package uk.gov.ida.notification;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.notification.resources.EidasAuthnRequestResource;
import uk.gov.ida.notification.saml.SamlParser;

import javax.xml.parsers.ParserConfigurationException;

public class EidasProxyNodeApplication extends Application<EidasProxyNodeConfiguration> {

    @SuppressWarnings("WeakerAccess") // Needed for DropwizardAppRules
    public EidasProxyNodeApplication() {
    }

    public static void main(final String[] args) throws Exception {
        if (args == null || args.length == 0) {
            String configFile = System.getenv("CONFIG_FILE");

            if (configFile == null) {
                throw new RuntimeException("CONFIG_FILE environment variable should be set with path to configuration file");
            }

            new EidasProxyNodeApplication().run("server", configFile);
        } else {
            new EidasProxyNodeApplication().run(args);
        }
    }

    @Override
    public String getName() {
        return "EidasProxyNode";
    }

    @Override
    public void initialize(final Bootstrap<EidasProxyNodeConfiguration> bootstrap) {
        // Needed to correctly interpolate environment variables in config file
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        // Needed to initialise OpenSAML libraries
        // The eidas-opensaml3 library provides its own initializer that will be executed
        // by the InitializationService
        try {
            InitializationService.initialize();
        } catch(InitializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(final EidasProxyNodeConfiguration configuration,
                    final Environment environment) {
        SamlParser samlParser;
        try {
            samlParser = new SamlParser();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        environment.jersey().register(new EidasAuthnRequestResource(samlParser));
    }

}
