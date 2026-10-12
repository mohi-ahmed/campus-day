package com.spark.campusday.lostfound.comment.service;

import com.spark.campusday.lostfound.comment.dto.LostFoundCommentPageResponse;
import com.spark.campusday.lostfound.comment.dto.LostFoundCommentRequest;
import com.spark.campusday.lostfound.comment.dto.LostFoundCommentResponse;
import com.spark.campusday.lostfound.comment.model.LostFoundComment;
import com.spark.campusday.lostfound.comment.repository.LostFoundCommentRepository;
import com.spark.campusday.lostfound.config.LostFoundCacheNames;
import com.spark.campusday.lostfound.post.service.LostFoundPostService;
import com.spark.campusday.lostfound.support.LostFoundInput;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LostFoundCommentService {
    private final LostFoundCommentRepository comments;
    private final LostFoundPostService posts;
    private final StudentService students;

    @CacheEvict(cacheNames = LostFoundCacheNames.COMMENTS, allEntries = true)
    public LostFoundCommentResponse create(String postId, LostFoundCommentRequest request) {
        posts.requireOpen(postId);
        Student author = students.getByStudentId(request.authorStudentId().trim());
        LostFoundComment saved = comments.save(LostFoundComment.builder()
                .postId(postId)
                .authorStudentId(author.getStudentId())
                .authorName(author.getName())
                .content(request.content().trim())
                .build());
        try {
            posts.incrementCommentCount(postId);
        } catch (RuntimeException exception) {
            comments.delete(saved);
            throw exception;
        }
        return response(saved);
    }

    @Cacheable(
            cacheNames = LostFoundCacheNames.COMMENTS,
            key = "#postId + ':' + #page + ':' + #size",
            sync = true)
    public LostFoundCommentPageResponse list(String postId, int page, int size) {
        LostFoundInput.validatePage(page, size);
        posts.requireOpen(postId);
        Page<LostFoundComment> result = comments.findByPostId(
                postId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
        return new LostFoundCommentPageResponse(
                result.getContent().stream().map(this::response).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }

    private LostFoundCommentResponse response(LostFoundComment comment) {
        return new LostFoundCommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorStudentId(),
                comment.getAuthorName(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
