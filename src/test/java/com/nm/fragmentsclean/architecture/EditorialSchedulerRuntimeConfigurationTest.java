package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EditorialSchedulerRuntimeConfigurationTest {
    @Test
    void staging_enables_discovery_and_analysis_with_their_intended_cadence() throws IOException {
        String bootstrap = Files.readString(Path.of(
                "infra/aws/compose/platform/staging/fragments/bootstrap-runtime.sh"));
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(bootstrap)
                .contains("EDITORIAL_DISCOVERY_SCHEDULE_ENABLED true")
                .contains("EDITORIAL_DISCOVERY_SCHEDULE_DELAY_MS 900000")
                .contains("EDITORIAL_ANALYSIS_SCHEDULE_ENABLED true")
                .contains("EDITORIAL_ANALYSIS_SCHEDULE_DELAY_MS 86400000");
        assertThat(properties)
                .contains("fragments.editorial.discovery.schedule.enabled=${EDITORIAL_DISCOVERY_SCHEDULE_ENABLED:false}")
                .contains("fragments.editorial.analysis.schedule.enabled=${EDITORIAL_ANALYSIS_SCHEDULE_ENABLED:false}");
    }
}
