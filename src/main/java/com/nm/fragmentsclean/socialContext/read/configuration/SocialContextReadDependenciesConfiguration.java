package com.nm.fragmentsclean.socialContext.read.configuration;


import com.nm.fragmentsclean.socialContext.read.GetLikeSummaryQueryHandler;
import com.nm.fragmentsclean.socialContext.read.ListCommentsQueryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SocialContextReadDependenciesConfiguration {

    @Bean
    GetLikeSummaryQueryHandler getLikeSummaryQueryHandler(
            JdbcTemplate jdbcTemplate
    ){
        return new GetLikeSummaryQueryHandler(jdbcTemplate);
    }

    @Bean
    ListCommentsQueryHandler listCommentsQueryHandler(JdbcTemplate jdbcTemplate){
        return new ListCommentsQueryHandler(jdbcTemplate);
    }
}
