package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaObjectKeys;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import jakarta.transaction.Transactional;import java.util.UUID;

public class RegisterAvatarUploadIntent{
  private final AppUserRepository users;private final AvatarMediaRepository media;private final PrivateMediaObjectKeys keys;private final DateTimeProvider clock;
  public RegisterAvatarUploadIntent(AppUserRepository users,AvatarMediaRepository media,PrivateMediaObjectKeys keys,DateTimeProvider clock){this.users=users;this.media=media;this.keys=keys;this.clock=clock;}
  @Transactional public AvatarMedia register(UUID mediaId,UUID userId,String type,long size){var existing=media.byId(mediaId);if(existing.isPresent()){if(!existing.get().matchesIntent(userId,type,size))throw new BusinessCommandRejectedException("AVATAR_ID_CONFLICT","Avatar id belongs to another upload intent");return existing.get();}var user=users.findById(userId).orElseThrow(()->new BusinessCommandRejectedException("APP_USER_NOT_FOUND","User not found"));if(user.lifecycleStatus()!=AppUserLifecycleStatus.ACTIVE)throw new BusinessCommandRejectedException("ACCOUNT_NOT_ACTIVE","Account is not active");var pending=AvatarMedia.pending(mediaId,userId,type,size,keys.pendingAvatar(userId,mediaId),clock.now());media.save(pending);return pending;}
}
