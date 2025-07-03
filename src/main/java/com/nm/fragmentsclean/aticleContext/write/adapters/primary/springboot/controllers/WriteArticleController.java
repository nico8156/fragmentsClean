package com.nm.fragmentsclean.aticleContext.write.adapters.primary.springboot.controllers;


import com.nm.fragmentsclean.aticleContext.write.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.CreateArticleCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/articles")
public class WriteArticleController {

    private final CommandBus commandBus;

    public WriteArticleController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    public record CreateArticleRequest (String title){ }

    @PostMapping
    public ResponseEntity<Void> CreateArticle(CreateArticleRequest createArticleRequest){
        commandBus.dispatch(new CreateArticleCommand(createArticleRequest.title()));
        return ResponseEntity.created(URI.create("/articles")).build();
    }
}
