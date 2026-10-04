package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.Optional;
import org.springframework.stereotype.Service;
@Service public class ReadAvatarMediaLifecycleQueryHandler {
  private final AvatarMediaLifecycleReadRepository repository;
  public ReadAvatarMediaLifecycleQueryHandler(AvatarMediaLifecycleReadRepository repository){this.repository=repository;}
  public Optional<AvatarMediaLifecycleView> handle(ReadAvatarMediaLifecycleQuery query){return repository.byId(query.mediaId());}
}
