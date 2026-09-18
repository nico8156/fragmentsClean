package com.nm.fragmentsclean.articleContext.write.adapters.secondary.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ArticleAuthoringHealthIndicatorTest {

  @Test
  void failed_saga_degrades_article_authoring_health_even_when_none_are_stale() {
    var indicator =
        new ArticleAuthoringHealthIndicator(
            new StubJdbcTemplate(0, 1), new SimpleMeterRegistry(), 15);

    var health = indicator.health();

    assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
    assertThat(health.getDetails())
        .containsEntry("staleActiveSagas", 0)
        .containsEntry("failedSagas", 1);
  }

  private static final class StubJdbcTemplate extends JdbcTemplate {
    private final int stale;
    private final int failed;

    private StubJdbcTemplate(int stale, int failed) {
      this.stale = stale;
      this.failed = failed;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
      return (T) Integer.valueOf(stale);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> requiredType) {
      return (T) Integer.valueOf(failed);
    }
  }
}
