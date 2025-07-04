package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.socialContext.write.adpaters.FakeLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MakeLikeCommandHandler implements CommandHandler<MakeLikeCommand> {

    FakeLikeRepository fakeLikeRepository;

    public MakeLikeCommandHandler(FakeLikeRepository fakeLikeRepository) {
        this.fakeLikeRepository = fakeLikeRepository;
    }

    @Override
    public void execute(MakeLikeCommand command) {
        //check if this like exist in the repo

        List<Like> existingLikesForUser = new ArrayList<>();



        Like likeToSave = new Like(
            command.likeId(),
            command.userId(),
            command.targetId()
        );
        fakeLikeRepository.save(likeToSave);

    }
}
