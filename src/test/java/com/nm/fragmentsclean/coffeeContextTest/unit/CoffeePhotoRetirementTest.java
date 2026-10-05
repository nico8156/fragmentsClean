package com.nm.fragmentsclean.coffeeContextTest.unit;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class CoffeePhotoRetirementTest {
    @Test void purge_requires_thirty_full_days_and_no_current_usage(){
        Instant retiredAt=Instant.parse("2026-10-04T10:00:00Z");
        var photo=new Photo(new PhotoId(UUID.randomUUID()),new CoffeeId(UUID.randomUUID()),"s3://private/coffees/photo.jpg",true,0);
        var retirement=new CoffeePhotoRetirement(photo,retiredAt);
        assertThat(retirement.photo()).isSameAs(photo);
        assertThat(retirement.retiredAt()).isEqualTo(retiredAt);
        assertThat(retirement.eligibleForPurge(retiredAt.plus(Duration.ofDays(30)).minusNanos(1),false)).isFalse();
        assertThat(retirement.eligibleForPurge(retiredAt.plus(Duration.ofDays(30)),false)).isTrue();
        assertThat(retirement.eligibleForPurge(retiredAt.plus(Duration.ofDays(31)),true)).isFalse();
    }
}
