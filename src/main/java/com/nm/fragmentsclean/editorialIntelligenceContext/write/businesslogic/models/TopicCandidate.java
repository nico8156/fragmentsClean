package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Editorial decision object. It references signals but never creates an article itself. */
public final class TopicCandidate {
    private final UUID id; private final String subject; private final String suggestedAngle;
    private final List<UUID> signalIds; private final Instant detectedAt; private TopicCandidateStatus status;
    private TopicCandidate(UUID id,String subject,String angle,List<UUID> signalIds,Instant detectedAt,TopicCandidateStatus status){this.id=Objects.requireNonNull(id);this.subject=text(subject);this.suggestedAngle=text(angle);this.signalIds=List.copyOf(signalIds);if(this.signalIds.isEmpty())throw new IllegalArgumentException("A topic candidate needs a source signal");this.detectedAt=Objects.requireNonNull(detectedAt);this.status=Objects.requireNonNull(status);}
    public static TopicCandidate detect(UUID id,String subject,String angle,List<UUID> signals,Instant now){return new TopicCandidate(id,subject,angle,signals,now,TopicCandidateStatus.DETECTED);}
    public static TopicCandidate reconstitute(Snapshot snapshot){return new TopicCandidate(snapshot.id(),snapshot.subject(),snapshot.suggestedAngle(),snapshot.signalIds(),snapshot.detectedAt(),snapshot.status());}
    public void retain(){transition(TopicCandidateStatus.RETAINED);} public void defer(){transition(TopicCandidateStatus.DEFERRED);} public void ignore(){transition(TopicCandidateStatus.IGNORED);}
    private void transition(TopicCandidateStatus target){if(status==TopicCandidateStatus.RETAINED&&target!=TopicCandidateStatus.RETAINED)throw new IllegalStateException("A retained candidate cannot be changed without an explicit editorial reset");status=target;}
    public Snapshot snapshot(){return new Snapshot(id,subject,suggestedAngle,signalIds,detectedAt,status);} private static String text(String v){if(v==null||v.isBlank())throw new IllegalArgumentException("Editorial text must not be blank");return v;}
    public record Snapshot(UUID id,String subject,String suggestedAngle,List<UUID> signalIds,Instant detectedAt,TopicCandidateStatus status){}
}
