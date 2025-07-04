package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.BlockType;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentBlock;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentValue;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.OrderBlock;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake.FakeContentBlockRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock.DeleteContentBlockCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock.DeleteContentBlockCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteContentBlockCommandHandlerTest {
    FakeContentBlockRepository fakeContentBlockRepository = new FakeContentBlockRepository();

    @BeforeEach
    void setup(){
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
    void shouldDeleteContentBlockGivenId(){
        new DeleteContentBlockCommandHandler(fakeContentBlockRepository).execute(
                new DeleteContentBlockCommand(
                        UUID.fromString("5E08C9F3-A2D5-490B-A471-490E138D9D19"))
        );
        assertThat(fakeContentBlockRepository.contentBlocks.size()).isEqualTo(0);
    }
}
