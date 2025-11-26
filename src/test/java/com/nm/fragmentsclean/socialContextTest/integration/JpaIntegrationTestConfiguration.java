package com.nm.fragmentsclean.socialContextTest.integration;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.JpaLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaIntegrationTestConfiguration {

    @Bean
    public LikeRepository likeRepository(SpringLikeRepository springLikeRepository) {
        return new JpaLikeRepository(springLikeRepository);
    }
//    @Bean
//    public CommentRepository commentRepository(SpringCommentRepository springCommentRepository) {
//        return new JpaCommentRepository(springCommentRepository);
//    }
}
