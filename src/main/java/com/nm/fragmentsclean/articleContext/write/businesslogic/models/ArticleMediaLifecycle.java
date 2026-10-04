package com.nm.fragmentsclean.articleContext.write.businesslogic.models;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
public record ArticleMediaLifecycle(UUID mediaId,UUID articleId,String reference,String status,long usages) {
 public static void requireActive(String status){if(!"ACTIVE".equals(status))throw new BusinessCommandRejectedException("MEDIA_RETIRED","An unavailable or retired article upload cannot be referenced");}
 public String transition(String requested){
  if(!"ACTIVE".equals(requested)&&!"RETIRED".equals(requested))throw new BusinessCommandRejectedException("MEDIA_STATUS_INVALID","Choose ACTIVE or RETIRED");
  if(!"ACTIVE".equals(status)&&!"RETIRED".equals(status))throw new BusinessCommandRejectedException("MEDIA_STATE_INVALID","Only active or retired media can change lifecycle");
  if("RETIRED".equals(requested)&&usages>0)throw new BusinessCommandRejectedException("MEDIA_IN_USE","Media is referenced by retained article revisions");
  return requested;
 }
}
