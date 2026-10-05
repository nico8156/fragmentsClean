package com.nm.fragmentsclean.userApplicationContextTest.unit;
import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class AvatarMediaLifecycleTest {
  final Instant now=Instant.parse("2026-10-04T10:00:00Z");
  AvatarMedia available(){var m=AvatarMedia.pending(UUID.randomUUID(),UUID.randomUUID(),"image/jpeg",1024,"pending",now);m.confirm("avatar/file","image/jpeg",1024,512,512,"hash",now);return m;}
  @Test void used_avatar_is_never_retired(){var m=available();assertThatThrownBy(()->m.retire(true,now)).hasMessageContaining("referenced");assertThat(m.snapshot().status()).isEqualTo(AvatarMediaStatus.AVAILABLE);}
  @Test void retirement_and_restoration_keep_metadata_and_are_idempotent(){var m=available();var before=m.snapshot();assertThat(m.retire(false,now)).isTrue();assertThat(m.retire(false,now)).isFalse();assertThat(m.snapshot().objectKey()).isEqualTo(before.objectKey());assertThat(m.snapshot().userId()).isEqualTo(before.userId());assertThat(m.restore(false,false,now)).isTrue();assertThat(m.restore(false,false,now)).isFalse();assertThat(m.snapshot().status()).isEqualTo(AvatarMediaStatus.AVAILABLE);assertThat(m.snapshot().version()).isEqualTo(before.version()+2);}
  @Test void deletion_states_and_replacement_conflicts_are_not_restorable(){var m=available();m.retire(false,now);assertThatThrownBy(()->m.restore(false,true,now)).hasMessageContaining("Another");assertThatThrownBy(()->m.restore(true,false,now)).hasMessageContaining("referenced");m.requestDeletion(now);assertThatThrownBy(()->m.restore(false,false,now)).hasMessageContaining("administratively");assertThatThrownBy(()->m.retire(false,now)).hasMessageContaining("available");}
  @Test void retired_avatar_cannot_be_confirmed_as_a_new_association(){var m=available();m.retire(false,now);assertThatThrownBy(()->m.confirm("avatar/file","image/jpeg",1024,512,512,"hash",now)).hasMessageContaining("cannot be confirmed");}
}
