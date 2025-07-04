package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.socialContext.write.adpaters.FakeLikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


public class MakeLikeCommandHandler implements CommandHandler<MakeLikeCommand> {

    FakeLikeRepository fakeLikeRepository;

    public MakeLikeCommandHandler(FakeLikeRepository fakeLikeRepository) {
        this.fakeLikeRepository = fakeLikeRepository;
    }

    @Override
    public void execute(MakeLikeCommand command) {
        //Check if like -with this userId exists?
        List<Like> likesWithUserId = fakeLikeRepository.likes.stream().filter(
                like -> like.toSnapshot().userId().equals(command.userId())
        ).collect(toList());
        //Si il y a des likes pour ce user, on cherche si la target(le cafe ) est deja present ?
        if(likesWithUserId.size() > 0){
            List<Like> likesWithUserIdAndTargetId =likesWithUserId.stream().filter(
                    like -> like.toSnapshot().targetId().equals(command.targetId())
            ).collect(Collectors.toList());
            System.out.println(likesWithUserIdAndTargetId.size());
            // si like present pour ce cafe on revoie une exception
            if (likesWithUserIdAndTargetId.size() > 0){
                throw new IllegalArgumentException("User already liked this article");
            }
        }

        Like likeToSave = new Like(
            command.likeId(),
            command.userId(),
            command.targetId()
        );
        fakeLikeRepository.save(likeToSave);

    }
}
