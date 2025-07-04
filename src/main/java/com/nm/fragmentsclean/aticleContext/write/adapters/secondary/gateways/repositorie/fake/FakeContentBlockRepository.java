package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentBlock;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ContentBlockRepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class FakeContentBlockRepository implements ContentBlockRepository {

    public List<ContentBlock> contentBlocks = new ArrayList<>();

    @Override
    public void save(ContentBlock contentBlock) {
        contentBlocks.add(contentBlock);
    }

    @Override
    public void delete(UUID id) {

        Iterator<ContentBlock> iterator = contentBlocks.iterator();

        while (iterator.hasNext()) {
            ContentBlock cb = iterator.next();
            if (cb.toSnapshot().id().equals(id)) {
                iterator.remove();
                break;
            }
        }
    }


    @Override
    public void update(ContentBlock contentBlock) {
        for (int i = 0; i < contentBlocks.size(); i++) {
            var blocToModify = contentBlocks.get(i);
            if(blocToModify.toSnapshot().id().equals(contentBlock.toSnapshot().id())){
                contentBlocks.set(i, contentBlock);
            }
        }
    }
}
