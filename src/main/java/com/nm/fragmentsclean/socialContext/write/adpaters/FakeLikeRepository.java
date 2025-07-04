package com.nm.fragmentsclean.socialContext.write.adpaters;

import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;

import java.util.ArrayList;
import java.util.List;

public class FakeLikeRepository implements LikeRepository {

    public List<Like> likes = new ArrayList<>();
    @Override
    public void save(Like like) {
        likes.add(like);
    }
}
