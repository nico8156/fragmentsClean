package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;
import java.time.*;import java.util.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
public final class CoffeePhotoRetirement {
 public static final Duration MINIMUM_RETENTION=Duration.ofDays(30);
 private final Photo photo;private final Instant retiredAt,purgeRequestedAt,purgedAt;private final String status;private final UUID purgeCommandId;
 public CoffeePhotoRetirement(Photo photo,Instant retiredAt){this(photo,retiredAt,"RETIRED",null,null,null);}
 public CoffeePhotoRetirement(Photo photo,Instant retiredAt,String status,Instant requestedAt,Instant purgedAt,UUID commandId){
  this.photo=Objects.requireNonNull(photo);this.retiredAt=Objects.requireNonNull(retiredAt);this.status=Objects.requireNonNull(status);this.purgeRequestedAt=requestedAt;this.purgedAt=purgedAt;this.purgeCommandId=commandId;
  if(!Set.of("RETIRED","DELETION_PENDING","DELETED").contains(status))throw new IllegalArgumentException("Invalid retired photo status");
 }
 public Photo photo(){return photo;}public Instant retiredAt(){return retiredAt;}public String status(){return status;}public Instant purgeRequestedAt(){return purgeRequestedAt;}public Instant purgedAt(){return purgedAt;}public UUID purgeCommandId(){return purgeCommandId;}
 public boolean eligibleForPurge(Instant now,boolean used){return "RETIRED".equals(status)&&!used&&!Objects.requireNonNull(now).isBefore(retiredAt.plus(MINIMUM_RETENTION));}
 public CoffeePhotoRetirement requestPurge(Instant now,boolean used,UUID commandId){
  if(used)throw reject("MEDIA_IN_USE");if("DELETION_PENDING".equals(status))return this;if(!"RETIRED".equals(status))throw reject("MEDIA_STATE_INVALID");
  if(!eligibleForPurge(now,false))throw reject("MEDIA_RETENTION_ACTIVE");return new CoffeePhotoRetirement(photo,retiredAt,"DELETION_PENDING",now,null,Objects.requireNonNull(commandId));
 }
 public CoffeePhotoRetirement completePurge(Instant now){if("DELETED".equals(status))return this;if(!"DELETION_PENDING".equals(status))throw reject("MEDIA_STATE_INVALID");return new CoffeePhotoRetirement(photo,retiredAt,"DELETED",purgeRequestedAt,Objects.requireNonNull(now),purgeCommandId);}
 private static BusinessCommandRejectedException reject(String code){return new BusinessCommandRejectedException(code,"Coffee photo purge rejected: "+code);}
}
