package com.nm.fragmentsclean.socialContext.read.configuration;


import com.nm.fragmentsclean.socialContext.read.GetLikeSummaryQueryHandler;
import com.nm.fragmentsclean.socialContext.read.ListCommentsQueryHandler;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
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
    @Bean
    public LikeRepository likeRepository(SpringLikeRepository springLikeRepository){
        return new JpaLikeRepository(springLikeRepository);
    }
    @Bean
    public CommentRepository commentRepository(SpringCommentRepository springCommentRepository){
        return new JpaCommentRepository(springCommentRepository);
    }
}
