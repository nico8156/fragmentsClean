package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake;

import com.nm.fragmentsclean.aticleContext.ContentBlock;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ContentBlockRepository;

import java.util.ArrayList;
import java.util.List;

public class FakeContentBlockRepository implements ContentBlockRepository {

    public List<ContentBlock> contentBlocks = new ArrayList<>();

    @Override
    public void save(ContentBlock contentBlock) {
        contentBlocks.add(contentBlock);
    }
}
