package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.BlockType;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentBlock;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentValue;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.OrderBlock;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake.FakeContentBlockRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock.UpdateContentBlockCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock.UpdateContentBlockCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

public class UpdateContentBlockCommandHandlerTest {
    FakeContentBlockRepository fakeContentBlockRepository = new FakeContentBlockRepository();

    @BeforeEach
    public void setUp() {
        fakeContentBlockRepository.save(
                new ContentBlock(
                        UUID.fromString("5E08C9F3-A2D5-490B-A471-490E138D9D19"),
                        UUID.fromString("175C4575-2BA5-4DB3-80CE-6422448C6D4D"),
                        BlockType.TITLE,
                        new ContentValue("Un article incroyable"),
                        new OrderBlock(1)

                )
        );
    }

    @Test
    public void shouldUpdateContentBlock() {
        new UpdateContentBlockCommandHandler(fakeContentBlockRepository).execute(
                new UpdateContentBlockCommand(
                        UUID.fromString("5E08C9F3-A2D5-490B-A471-490E138D9D19"),
                        UUID.fromString("175C4575-2BA5-4DB3-80CE-6422448C6D4D"),
                        BlockType.TITLE,
                        new ContentValue("Un article phénomenal"),
                        new OrderBlock(1)

                )
        );
        assertThat (fakeContentBlockRepository.contentBlocks.size()).isEqualTo(1);
        assertThat (fakeContentBlockRepository.contentBlocks.stream().map(
                ContentBlock::toSnapshot
        )).containsExactly(
                new ContentBlock.ContentBlockSnapshot(
                        UUID.fromString("5E08C9F3-A2D5-490B-A471-490E138D9D19"),
                        UUID.fromString("175C4575-2BA5-4DB3-80CE-6422448C6D4D"),
                        BlockType.TITLE,
                        new ContentValue("Un article phénomenal"),
                        new OrderBlock(1)
                )
        );

    }
}
