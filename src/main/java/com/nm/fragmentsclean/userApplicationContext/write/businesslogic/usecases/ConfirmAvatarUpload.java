package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.DurableCommandExecutor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaObjectKeys;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AvatarMediaRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMediaStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ConfirmAvatarUpload {
  private static final PrivateImageStore.ImageRules RULES = new PrivateImageStore.ImageRules(
      8_000_000, 40_000_000, 512, 512, true, .86f);
  private final AvatarMediaRepository media;
  private final PrivateImageStore store;
  private final PrivateMediaObjectKeys keys;
  private final DurableCommandExecutor durable;
  private final ConfirmAvatarCommandHandler handler;
  public ConfirmAvatarUpload(AvatarMediaRepository media,PrivateImageStore store,PrivateMediaObjectKeys keys,DurableCommandExecutor durable,ConfirmAvatarCommandHandler handler){this.media=media;this.store=store;this.keys=keys;this.durable=durable;this.handler=handler;}
  public void confirm(UUID commandId,UUID mediaId,UUID userId,Instant clientAt){var pending=media.inspect(mediaId).orElseThrow(()->new BusinessCommandRejectedException("AVATAR_NOT_FOUND","Avatar upload intent does not exist"));if(!Objects.equals(pending.userId(),userId))throw new BusinessCommandRejectedException("AVATAR_FORBIDDEN","Avatar upload is not owned by requester");PrivateImageStore.ProcessedImage processed;if(pending.status()==AvatarMediaStatus.AVAILABLE){processed=new PrivateImageStore.ProcessedImage(pending.objectKey(),pending.contentType(),pending.size(),pending.width(),pending.height(),pending.sha256());}else if(pending.status()==AvatarMediaStatus.PENDING){processed=store.normalize(pending.pendingObjectKey(),keys.avatar(userId,mediaId),pending.declaredContentType(),RULES);}else throw new BusinessCommandRejectedException("AVATAR_NOT_PENDING","Avatar upload cannot be confirmed");var command=new ConfirmAvatarCommand(commandId,mediaId,userId,processed.objectKey(),processed.contentType(),processed.size(),processed.width(),processed.height(),processed.sha256(),clientAt);durable.execute(command,()->handler.execute(command));store.delete(pending.pendingObjectKey());}
}
