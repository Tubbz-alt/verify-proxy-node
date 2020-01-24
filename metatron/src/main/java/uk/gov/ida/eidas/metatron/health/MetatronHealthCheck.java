package uk.gov.ida.eidas.metatron.health;

import com.codahale.metrics.health.HealthCheck;

public class MetatronHealthCheck extends HealthCheck {
    @Override
    protected Result check() {
        return Result.healthy();
    }
}
