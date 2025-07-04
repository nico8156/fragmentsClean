package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.BlockType;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentBlock;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentValue;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.OrderBlock;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake.FakeContentBlockRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock.CreateContenBlockCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock.CreateContentBlockCommandHandler;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


public class CreateContentBlockCommandHandlerTest {

    FakeContentBlockRepository contentBlockRepository = new FakeContentBlockRepository();

    @Test
    public void shouldCreateContentBlock() {

        new CreateContentBlockCommandHandler(contentBlockRepository).execute(
                new CreateContenBlockCommand(
                        UUID.fromString("FA9100B8-7F1E-44BA-9292-E70A43A79F2C"),
                        UUID.fromString("79C9272C-3C91-4CE9-A8A5-494D35E947BE"),
                        BlockType.TITLE,
                        "Un article incroyable",
                        1
                )
        );
        assertThat(contentBlockRepository.contentBlocks.size()).isEqualTo(1);
        assertThat(contentBlockRepository.contentBlocks.stream().map(
                ContentBlock::toSnapshot
        )).containsExactly(
                new ContentBlock.ContentBlockSnapshot(
                        UUID.fromString("FA9100B8-7F1E-44BA-9292-E70A43A79F2C"),
                        UUID.fromString("79C9272C-3C91-4CE9-A8A5-494D35E947BE"),
                        BlockType.TITLE,
                        new ContentValue("Un article incroyable"),
                        new OrderBlock(1)
                )
        );
    }
}
