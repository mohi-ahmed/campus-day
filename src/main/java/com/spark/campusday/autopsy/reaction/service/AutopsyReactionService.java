package com.spark.campusday.autopsy.reaction.service;

import com.spark.campusday.autopsy.post.model.AutopsyPost;
import com.spark.campusday.autopsy.post.service.AutopsyPostService;
import com.spark.campusday.autopsy.reaction.dto.ReactionRequest;
import com.spark.campusday.autopsy.reaction.dto.ReactionResponse;
import com.spark.campusday.autopsy.reaction.dto.RepostResponse;
import com.spark.campusday.autopsy.reaction.dto.SessionRequest;
import com.spark.campusday.autopsy.reaction.model.AutopsyEngagement;
import com.spark.campusday.autopsy.reaction.model.AutopsyReactionType;
import com.spark.campusday.autopsy.reaction.repository.AutopsyEngagementRepository;
import com.spark.campusday.autopsy.support.AutopsyInput;
import com.spark.campusday.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AutopsyReactionService {
    private final AutopsyEngagementRepository engagements;
    private final AutopsyPostService posts;

    public ReactionResponse react(String postId, ReactionRequest request) {
        posts.requireActive(postId);
        String actorHash = AutopsyInput.actorHash(request.sessionId());
        AutopsyEngagement current = engagements.findByPostIdAndActorHash(postId, actorHash)
                .orElseGet(() -> newEngagement(postId, actorHash));
        AutopsyReactionType previous = current.getReaction();
        AutopsyReactionType selected = previous == request.reaction() ? null : request.reaction();

        persist(current.toBuilder().reaction(selected).build());
        AutopsyPost updated = posts.applyReactionChange(
                postId,
                previous == null ? null : previous.name(),
                selected == null ? null : selected.name());
        return new ReactionResponse(
                postId,
                selected,
                updated.getTotalReactionCount(),
                reactionCounts(updated));
    }

    public RepostResponse toggleRepost(String postId, SessionRequest request) {
        posts.requireActive(postId);
        String actorHash = AutopsyInput.actorHash(request.sessionId());
        AutopsyEngagement current = engagements.findByPostIdAndActorHash(postId, actorHash)
                .orElseGet(() -> newEngagement(postId, actorHash));
        boolean reposted = !current.isReposted();

        persist(current.toBuilder().reposted(reposted).build());
        AutopsyPost updated = posts.updateRepostCount(postId, reposted);
        return new RepostResponse(postId, reposted, updated.getRepostCount());
    }

    private AutopsyEngagement newEngagement(String postId, String actorHash) {
        return AutopsyEngagement.builder()
                .postId(postId)
                .actorHash(actorHash)
                .build();
    }

    private void persist(AutopsyEngagement engagement) {
        try {
            if (engagement.getReaction() == null && !engagement.isReposted() && engagement.getId() != null) {
                engagements.delete(engagement);
            } else if (engagement.getReaction() != null || engagement.isReposted()) {
                engagements.save(engagement);
            }
        } catch (DuplicateKeyException | OptimisticLockingFailureException exception) {
            throw new ConflictException(
                    "This anonymous interaction changed concurrently. Please retry",
                    exception);
        }
    }

    private Map<String, Long> reactionCounts(AutopsyPost post) {
        Map<String, Long> counts = post.getReactionCounts();
        return counts == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }
}
