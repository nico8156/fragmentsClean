package com.nm.fragmentsclean.articleContext.write.businesslogic.models;
import java.util.UUID;
import java.time.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
public record ArticleMediaLifecycle(UUID mediaId,UUID articleId,String reference,String status,long usages,Instant updatedAt) {
 public static final Duration MINIMUM_RETENTION=Duration.ofDays(30);
 public ArticleMediaLifecycle(UUID mediaId,UUID articleId,String reference,String status,long usages){this(mediaId,articleId,reference,status,usages,null);}
 public static void requireActive(String status){if(!"ACTIVE".equals(status))throw new BusinessCommandRejectedException("MEDIA_RETIRED","An unavailable or retired article upload cannot be referenced");}
 public boolean canPurge(Instant now){return "RETIRED".equals(status)&&usages==0&&updatedAt!=null&&!now.isBefore(updatedAt.plus(MINIMUM_RETENTION));}
 public String transition(String requested){return transition(requested,null);}
 public String transition(String requested,Instant now){
  if("PURGE_REQUESTED".equals(requested)){
   if(usages>0)throw new BusinessCommandRejectedException("MEDIA_IN_USE","Media is referenced by retained article revisions");
   if("DELETION_PENDING".equals(status))return status;
   if(!"RETIRED".equals(status))throw new BusinessCommandRejectedException("MEDIA_STATE_INVALID","Only retired media can request purge");
   if(now==null||!canPurge(now))throw new BusinessCommandRejectedException("MEDIA_RETENTION_ACTIVE","Retain retired media for 30 full days");
   return "DELETION_PENDING";
  }
  if(!"ACTIVE".equals(requested)&&!"RETIRED".equals(requested))throw new BusinessCommandRejectedException("MEDIA_STATUS_INVALID","Choose ACTIVE or RETIRED");
  if(!"ACTIVE".equals(status)&&!"RETIRED".equals(status))throw new BusinessCommandRejectedException("MEDIA_STATE_INVALID","Only active or retired media can change lifecycle");
  if("RETIRED".equals(requested)&&usages>0)throw new BusinessCommandRejectedException("MEDIA_IN_USE","Media is referenced by retained article revisions");
  return requested;
 }
}
