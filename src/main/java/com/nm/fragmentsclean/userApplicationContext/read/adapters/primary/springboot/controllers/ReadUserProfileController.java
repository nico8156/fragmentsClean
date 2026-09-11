package com.nm.fragmentsclean.userApplicationContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.userApplicationContext.read.GetAppUserProfileQuery;
import com.nm.fragmentsclean.userApplicationContext.read.projections.AppUserProfileView;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public final class ReadUserProfileController {
  private final QueryBus queryBus;

  public ReadUserProfileController(QueryBus queryBus) {
    this.queryBus = queryBus;
  }

  @GetMapping
  public ResponseEntity<AppUserProfileView> me(@AuthenticationPrincipal Jwt jwt) {
    var view = queryBus.dispatch(new GetAppUserProfileQuery(UUID.fromString(jwt.getSubject())));
    return view == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(view);
  }
}
