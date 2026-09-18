package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.Status;

class ReleaseHealthConfigurationTest {

  @Test
  void production_exposes_a_strict_release_group_without_coupling_liveness_to_business_health()
      throws Exception {
    var properties = new Properties();
    try (InputStream input =
        getClass().getResourceAsStream("/application-prod.properties")) {
      properties.load(input);
    }

    String statusOrder = properties.getProperty("management.endpoint.health.status.order");
    assertThat(statusOrder).isEqualTo("DOWN,OUT_OF_SERVICE,DEGRADED,UNKNOWN,UP");
    var aggregator = new SimpleStatusAggregator(Arrays.asList(statusOrder.split(",")));
    assertThat(
            aggregator.getAggregateStatus(
                java.util.Set.of(Status.UP, new Status("DEGRADED"))))
        .isEqualTo(new Status("DEGRADED"));
    assertThat(properties.getProperty("management.endpoint.health.group.release.include"))
        .isEqualTo(
            "db,messagingRuntimeHealth,articleAuthoringHealth,ticketVerificationHealth,editorialOperationsHealth");
    assertThat(properties.getProperty("management.endpoint.health.group.liveness.include"))
        .isNull();
  }
}
