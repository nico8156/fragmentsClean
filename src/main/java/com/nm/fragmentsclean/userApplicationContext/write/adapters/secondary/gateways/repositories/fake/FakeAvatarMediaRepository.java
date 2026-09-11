package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.fake;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AvatarMediaRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import java.time.Instant;import java.util.*;

public final class FakeAvatarMediaRepository implements AvatarMediaRepository{
  private final Map<UUID,AvatarMedia.Snapshot> rows=new HashMap<>();
  public Optional<AvatarMedia> byId(UUID id){return Optional.ofNullable(rows.get(id)).map(AvatarMedia::fromSnapshot);}
  public Optional<AvatarMedia.Snapshot> inspect(UUID id){return Optional.ofNullable(rows.get(id));}
  public Optional<AvatarMedia> activeByUser(UUID id){return rows.values().stream().filter(s->Objects.equals(s.userId(),id)&&s.status()==AvatarMediaStatus.AVAILABLE).findFirst().map(AvatarMedia::fromSnapshot);}
  public void save(AvatarMedia media){rows.put(media.id(),media.snapshot());}
  public void replaceActive(AvatarMedia previous,AvatarMedia replacement){save(previous);save(replacement);}
  public List<AvatarMedia> cleanupCandidates(Instant before,int limit){return rows.values().stream().filter(s->s.status()==AvatarMediaStatus.DELETION_PENDING||(s.status()==AvatarMediaStatus.PENDING&&s.updatedAt().isBefore(before))).limit(limit).map(AvatarMedia::fromSnapshot).toList();}
}
