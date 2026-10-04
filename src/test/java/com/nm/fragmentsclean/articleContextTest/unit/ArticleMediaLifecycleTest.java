package com.nm.fragmentsclean.articleContextTest.unit;
import org.junit.jupiter.api.Test;import java.util.UUID;import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaLifecycle;import static org.assertj.core.api.Assertions.*;
class ArticleMediaLifecycleTest {
 @Test void retained_usages_block_retirement_but_unused_media_can_be_restored(){var used=new ArticleMediaLifecycle(UUID.randomUUID(),UUID.randomUUID(),"ref","ACTIVE",1);assertThatThrownBy(()->used.transition("RETIRED")).hasMessageContaining("referenced");var retired=new ArticleMediaLifecycle(used.mediaId(),used.articleId(),"ref","RETIRED",0);assertThat(retired.transition("ACTIVE")).isEqualTo("ACTIVE");}
 @Test void retired_references_are_not_available_and_unknown_decisions_are_rejected(){assertThatThrownBy(()->ArticleMediaLifecycle.requireActive("RETIRED")).hasMessageContaining("retired");assertThatCode(()->ArticleMediaLifecycle.requireActive("ACTIVE")).doesNotThrowAnyException();var active=new ArticleMediaLifecycle(UUID.randomUUID(),UUID.randomUUID(),"ref","ACTIVE",0);assertThatThrownBy(()->active.transition("DELETED")).hasMessageContaining("ACTIVE or RETIRED");assertThat(active.transition("RETIRED")).isEqualTo("RETIRED");}
 @Test void only_active_media_can_be_bound_and_terminal_states_cannot_be_restored(){
  for(String state:new String[]{"DELETION_PENDING","DELETED","UNKNOWN"}){
   assertThatThrownBy(()->ArticleMediaLifecycle.requireActive(state)).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
   var media=new ArticleMediaLifecycle(UUID.randomUUID(),UUID.randomUUID(),"ref",state,0);
   assertThatThrownBy(()->media.transition("ACTIVE")).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
  }
 }
}
