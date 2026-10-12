package com.spark.campusday.autopsy.comment.service;

import com.spark.campusday.autopsy.comment.dto.CommentPage;
import com.spark.campusday.autopsy.comment.dto.CommentRequest;
import com.spark.campusday.autopsy.comment.dto.CommentResponse;
import com.spark.campusday.autopsy.comment.model.AutopsyComment;
import com.spark.campusday.autopsy.comment.repository.AutopsyCommentRepository;
import com.spark.campusday.autopsy.post.service.AutopsyPostService;
import com.spark.campusday.autopsy.support.AutopsyInput;
import com.spark.campusday.autopsy.support.AnonymousActorFingerprint;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutopsyCommentService {
    private final AutopsyCommentRepository comments;
    private final AutopsyPostService posts;
    private final CommentCreationRateLimit creationRateLimit;
    private final AnonymousActorFingerprint actorFingerprint;

    public CommentResponse create(
            String postId,
            CommentRequest request,
            String clientIp,
            String userAgent) {
        String actorKey = actorFingerprint.create(request.sessionId(), clientIp, userAgent);
        return creationRateLimit.execute(actorKey, () -> save(postId, request));
    }

    private CommentResponse save(String postId, CommentRequest request) {
        posts.requireActive(postId);
        AutopsyComment saved = comments.save(AutopsyComment.builder()
                .postId(postId)
                .content(request.content().trim())
                .nickname(AutopsyInput.nickname(request.nickname()))
                .build());
        try {
            posts.incrementCommentCount(postId);
        } catch (RuntimeException exception) {
            comments.delete(saved);
            throw exception;
        }
        return response(saved);
    }

    public CommentPage list(String postId, int page, int size) {
        AutopsyInput.validatePage(page, size);
        posts.requireActive(postId);
        Page<AutopsyComment> result = comments.findByPostId(
                postId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
        return new CommentPage(
                result.getContent().stream().map(this::response).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }

    private CommentResponse response(AutopsyComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getContent(),
                comment.getNickname(),
                comment.getCreatedAt());
    }
}
