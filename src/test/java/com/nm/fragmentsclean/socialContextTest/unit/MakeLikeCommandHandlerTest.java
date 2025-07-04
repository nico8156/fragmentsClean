package com.nm.fragmentsclean.socialContextTest.unit;

import com.nm.fragmentsclean.socialContext.write.adpaters.FakeLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommandHandler;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MakeLikeCommandHandlerTest {

    FakeLikeRepository fakeLikeRepository = new FakeLikeRepository();

    @Test
    void shouldMakeALike() {

        new MakeLikeCommandHandler(fakeLikeRepository).execute(
                new MakeLikeCommand(
                        UUID.fromString("70E37428-0268-4683-B7CD-4F06C13E83D2"),
                        UUID.fromString("175C4575-2BA5-4DB3-80CE-6422448C6D4D"),
                        UUID.fromString("5E08C9F3-A2D5-490B-A471-490E138D9D19")
                )
        );

        assertThat(fakeLikeRepository.likes).size().isEqualTo(1);
        assertThat(fakeLikeRepository.likes.stream().map(Like::toSnapshot)).containsExactly(
                new Like.LikeSnapshot(
                        UUID.fromString("70E37428-0268-4683-B7CD-4F06C13E83D2"),
                        UUID.fromString("175C4575-2BA5-4DB3-80CE-6422448C6D4D"),
                        UUID.fromString("5E08C9F3-A2D5-490B-A471-490E138D9D19")
                )
        );
    }
    @Test
    void shouldNotMakeALikeIfAlreadyLiked() {
        fakeLikeRepository.likes.add(new Like(
                UUID.fromString("70E37428-0268-4683-B7CD-4F06C13E83D2"),
                UUID.fromString("175C4575-2BA5-4DB3-80CE-6422448C6D4D"),
                UUID.fromString("5E08C9F3-A2D5-490B-A471-490E138D9D19")
        ));

        new MakeLikeCommandHandler(fakeLikeRepository).execute(
                new MakeLikeCommand(
                        UUID.fromString("70E37428-0268-4683-B7CD-4F06C13E83D2"),
                        UUID.fromString("175C4575-2BA5-4DB3-80CE-6422448C6D4D"),
                        UUID.fromString("5E08C9F3-A2D5-490B-A471-490E138D9D19")
                )
        );

    }

}
