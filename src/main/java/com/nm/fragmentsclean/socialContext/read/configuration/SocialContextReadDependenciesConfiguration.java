package com.nm.fragmentsclean.socialContext.read.configuration;

import com.nm.fragmentsclean.socialContext.read.GetLikeSummaryQueryHandler;
import com.nm.fragmentsclean.socialContext.read.ListCommentsQueryHandler;
import com.nm.fragmentsclean.socialContext.read.ListBlockedUsersQueryHandler;
import com.nm.fragmentsclean.socialContext.read.ListModerationReportsQueryHandler;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcLikeProjectionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SocialContextReadDependenciesConfiguration {

  @Bean
  GetLikeSummaryQueryHandler getLikeSummaryQueryHandler(
      JdbcLikeProjectionRepository projectionRepository) {
    return new GetLikeSummaryQueryHandler(projectionRepository);
  }

  @Bean
  ListCommentsQueryHandler listCommentsQueryHandler(JdbcTemplate jdbcTemplate,com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaUrlResolver mediaUrls) {
    return new ListCommentsQueryHandler(jdbcTemplate,mediaUrls);
  }

  @Bean ListBlockedUsersQueryHandler listBlockedUsersQueryHandler(JdbcTemplate jdbc,com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaUrlResolver mediaUrls) {
    return new ListBlockedUsersQueryHandler(jdbc,mediaUrls);
  }

  @Bean ListModerationReportsQueryHandler listModerationReportsQueryHandler(JdbcTemplate jdbc) {
    return new ListModerationReportsQueryHandler(jdbc);
  }

}
