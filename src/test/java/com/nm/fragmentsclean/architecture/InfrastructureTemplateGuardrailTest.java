package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations;
import org.junit.jupiter.api.Test;

class InfrastructureTemplateGuardrailTest {

    private static final Path STAGING_TEMPLATE =
            Path.of("infra/aws/cloudformation/staging-minimal.yaml");

    @Test
    void staging_images_are_explicit_operator_inputs_without_a_moving_default() throws IOException {
        for (Path path : List.of(STAGING_TEMPLATE,
                Path.of("infra/aws/cloudformation/platform-staging.yaml"))) {
            String template = Files.readString(path);
            assertThat(template).contains("      ImageId: !Ref InstanceImageId\n")
                    .doesNotContain("{{resolve:ssm:");
            var parameter = java.util.regex.Pattern.compile(
                    "(?ms)^  InstanceImageId:\\n(.*?)(?=^  [A-Za-z]|^Resources:|^Conditions:)")
                    .matcher(template);
            assertThat(parameter.find()).as("Explicit image parameter in %s", path).isTrue();
            assertThat(parameter.group(1)).contains("Type: AWS::EC2::Image::Id")
                    .doesNotContain("Default:");
        }
    }

    @Test
    void release_preserves_the_deployed_legacy_github_role() throws Exception {
        String template = Files.readString(STAGING_TEMPLATE);
        String role = template.substring(template.indexOf("  GitHubDeployRole:\n"),
                template.indexOf("\nOutputs:\n")) + "\n";
        // Snapshot of the actual deployed role, read 2026-09-12. Its retirement
        // is a separate operator decision, not part of the messaging release.
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(role.getBytes(StandardCharsets.UTF_8)));
        assertThat(checksum).isEqualTo(
                "6703be10d6d72c1839eb784eb9de3d2b84963b52f5d4c67d64ba7ee35bf1f2e1");
    }

    @Test
    void every_source_queue_has_an_owned_dead_letter_queue_and_an_age_alarm() throws Exception {
        String template = Files.readString(STAGING_TEMPLATE);
        // Drive coverage from the production catalog, not a second list which
        // could forget a newly introduced bounded-context destination.
        for (var field : IntegrationEventDestinations.class.getFields()) {
            if (field.getType() != String.class) continue;
            String wireName = (String) field.get(null);
            String destination = Arrays.stream(wireName.split("-"))
                    .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                    .collect(Collectors.joining());
            assertThat(template)
                    .as("Infrastructure for %s", wireName)
                    .contains("QueueName: !Sub ${ProjectName}-${EnvironmentName}-" + wireName + "\n")
                    .contains(destination + "DeadLetterQueue:")
                    .contains("deadLetterTargetArn: !GetAtt " + destination + "DeadLetterQueue.Arn")
                    .contains(destination + "DeadLetterQueueDepthAlarm:")
                    .contains(destination + "OldestMessageAlarm:");
        }

        assertThat(template)
                .doesNotContain("deadLetterTargetArn: !GetAtt SharedDeadLetterQueue.Arn")
                .contains("LegacySharedDeadLetterQueueDepthAlarm:")
                .contains("Value: legacy-awaiting-triage");
    }

    @Test
    void every_catalog_destination_is_wired_into_the_deployed_environment() throws Exception {
        String bootstrap = Files.readString(Path.of(
                "infra/aws/compose/platform/staging/fragments/bootstrap-runtime.sh"));
        String example = Files.readString(Path.of("infra/aws/compose/staging/.env.example"));
        String application = Files.readString(Path.of("src/main/resources/application.properties"));
        for (var field : IntegrationEventDestinations.class.getFields()) {
            if (field.getType() != String.class) continue;
            String wireName = (String) field.get(null);
            String variable = "SQS_" + wireName.toUpperCase(Locale.ROOT).replace('-', '_') + "_URL";
            assertThat(application).contains("app.messaging.sqs.queues." + wireName + "=${" + variable + ":}");
            assertThat(bootstrap).contains("write_env " + variable + " ");
            assertThat(example).contains(variable + "=");
        }
    }

    @Test
    void operations_alarms_are_routed_to_the_operator_topic() throws IOException {
        String template = Files.readString(STAGING_TEMPLATE);

        assertThat(template)
                .contains("OperationsAlarmTopic:")
                .contains("OperationsAlarmEmailSubscription:")
                .contains("Condition: HasOperationsAlarmEmail")
                .contains("AlarmActions: [!Ref OperationsAlarmTopic]")
                .contains("MetricName: ApproximateAgeOfOldestMessage")
                .contains("MetricName: ApproximateNumberOfMessagesVisible");
    }

    @Test
    void editorial_health_has_a_scoped_cloudwatch_metric_and_operator_alarm() throws IOException {
        String template = Files.readString(STAGING_TEMPLATE);
        String platform = Files.readString(Path.of("infra/aws/cloudformation/platform-staging.yaml"));
        String deployment = Files.readString(Path.of(
                "infra/aws/compose/platform/staging/fragments/deploy-via-ssm.sh"));

        assertThat(template)
                .contains("EditorialOperationsDegradedAlarm:")
                .contains("MetricName: EditorialOperationsDegraded")
                .contains("cloudwatch:PutMetricData")
                .contains("cloudwatch:namespace: Fragments/Staging")
                .contains("TreatMissingData: breaching");
        assertThat(deployment)
                .contains("publish-editorial-health.sh")
                .contains("fragments-editorial-health.timer")
                .contains("systemctl enable --now fragments-editorial-health.timer");
        assertThat(platform)
                .contains("cloudwatch:PutMetricData")
                .contains("cloudwatch:namespace: Fragments/Staging");
    }

    @Test
    void private_media_and_release_limits_are_least_privilege_and_enabled_in_staging() throws IOException {
        String minimal = Files.readString(STAGING_TEMPLATE);
        String platform = Files.readString(Path.of("infra/aws/cloudformation/platform-staging.yaml"));
        String bootstrap = Files.readString(Path.of(
                "infra/aws/compose/platform/staging/fragments/bootstrap-runtime.sh"));
        String example = Files.readString(Path.of("infra/aws/compose/staging/.env.example"));

        for (String template : List.of(minimal, platform)) {
            assertThat(template)
                    .contains("fragments/staging/coffees/*")
                    .contains("fragments/staging/articles/*")
                    .contains("fragments/staging/private-media/*")
                    .contains("fragments/staging/backups/postgres/*")
                    .doesNotContain("/fragments/staging/*");
        }
        for (String runtime : List.of(bootstrap, example)) {
            assertThat(runtime)
                    .contains("FRAGMENTS_RATE_LIMIT_ENABLED")
                    .contains("FRAGMENTS_RATE_LIMIT_TICKET_PER_MINUTE")
                    .contains("FRAGMENTS_RATE_LIMIT_MEDIA_PER_MINUTE")
                    .contains("FRAGMENTS_RATE_LIMIT_UGC_PER_MINUTE")
                    .contains("TICKETVERIFY_HEALTH_STALE_AFTER_SECONDS");
        }
    }
}
