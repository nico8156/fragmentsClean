package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class InfrastructureTemplateGuardrailTest {

    private static final Path STAGING_TEMPLATE =
            Path.of("infra/aws/cloudformation/staging-minimal.yaml");

    @Test
    void every_source_queue_has_an_owned_dead_letter_queue_and_an_age_alarm() throws IOException {
        String template = Files.readString(STAGING_TEMPLATE);
        List<String> destinations = List.of(
                "ArticlesEvents",
                "AuthUsersEvents",
                "AppUsersEvents",
                "CoffeesEvents",
                "DomainEvents",
                "TicketEvents",
                "TicketVerificationRequested");

        for (String destination : destinations) {
            assertThat(template)
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
    }
}
