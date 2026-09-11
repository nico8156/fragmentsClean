package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMedia;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvatarMediaRepository {
  Optional<AvatarMedia> byId(UUID mediaId);
  Optional<AvatarMedia.Snapshot> inspect(UUID mediaId);
  Optional<AvatarMedia> activeByUser(UUID userId);
  void save(AvatarMedia media);
  void replaceActive(AvatarMedia previous, AvatarMedia replacement);
  List<AvatarMedia> cleanupCandidates(Instant pendingBefore,int limit);
}
