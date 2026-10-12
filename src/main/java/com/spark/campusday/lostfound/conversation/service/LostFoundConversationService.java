package com.spark.campusday.lostfound.conversation.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.lostfound.conversation.dto.LostFoundConversationResponse;
import com.spark.campusday.lostfound.conversation.dto.LostFoundMessageRequest;
import com.spark.campusday.lostfound.conversation.dto.LostFoundMessageResponse;
import com.spark.campusday.lostfound.conversation.dto.LostFoundThreadResponse;
import com.spark.campusday.lostfound.conversation.dto.LostFoundUnreadCountResponse;
import com.spark.campusday.lostfound.conversation.model.LostFoundConversation;
import com.spark.campusday.lostfound.conversation.model.LostFoundMessage;
import com.spark.campusday.lostfound.conversation.repository.LostFoundConversationRepository;
import com.spark.campusday.lostfound.conversation.repository.LostFoundMessageRepository;
import com.spark.campusday.lostfound.post.model.LostFoundPost;
import com.spark.campusday.lostfound.post.service.LostFoundPostService;
import com.spark.campusday.lostfound.support.LostFoundInput;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LostFoundConversationService {
    private final LostFoundConversationRepository conversations;
    private final LostFoundMessageRepository messages;
    private final LostFoundPostService posts;
    private final StudentService students;
    private final MongoTemplate mongoTemplate;

    public LostFoundMessageResponse start(String postId, LostFoundMessageRequest request) {
        LostFoundPost post = posts.requireOpen(postId);
        Student sender = students.getByStudentId(request.senderStudentId().trim());
        if (post.getOwnerStudentId().equals(sender.getStudentId())) {
            throw new InvalidRequestException("You cannot contact yourself about your own post");
        }
        LostFoundConversation conversation = conversations
                .findByPostIdAndParticipantStudentId(postId, sender.getStudentId())
                .orElseGet(() -> createConversation(post, sender));
        return saveMessage(conversation, sender, request.content());
    }

    public LostFoundMessageResponse reply(String conversationId, LostFoundMessageRequest request) {
        Student sender = students.getByStudentId(request.senderStudentId().trim());
        LostFoundConversation conversation = requireConversation(conversationId);
        requireMember(conversation, sender.getStudentId());
        return saveMessage(conversation, sender, request.content());
    }

    public List<LostFoundConversationResponse> inbox(String studentId) {
        String userId = verifiedStudentId(studentId);
        List<LostFoundConversation> userConversations = conversations
                .findByOwnerStudentIdOrParticipantStudentIdOrderByLastMessageAtDesc(userId, userId);
        Set<String> postIds = userConversations.stream()
                .map(LostFoundConversation::getPostId)
                .collect(Collectors.toSet());
        Map<String, LostFoundPost> postById = posts.findByIds(postIds);
        return userConversations.stream()
                .filter(conversation -> postById.containsKey(conversation.getPostId()))
                .map(conversation -> response(conversation, postById.get(conversation.getPostId()), userId))
                .toList();
    }

    public LostFoundThreadResponse thread(String conversationId, String studentId, int page, int size) {
        LostFoundInput.validatePage(page, size);
        String userId = verifiedStudentId(studentId);
        LostFoundConversation conversation = requireConversation(conversationId);
        requireMember(conversation, userId);
        LostFoundPost post = posts.findByIds(Set.of(conversation.getPostId()))
                .get(conversation.getPostId());
        if (post == null) {
            throw new ResourceNotFoundException("Lost and found post not found: " + conversation.getPostId());
        }

        Page<LostFoundMessage> result = messages.findByConversationId(
                conversationId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
        List<LostFoundMessage> chronological = new ArrayList<>(result.getContent());
        java.util.Collections.reverse(chronological);
        return new LostFoundThreadResponse(
                response(conversation, post, userId),
                chronological.stream().map(this::messageResponse).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }

    public LostFoundUnreadCountResponse unreadCount(String studentId) {
        String userId = verifiedStudentId(studentId);
        long count = conversations
                .findByOwnerStudentIdOrParticipantStudentIdOrderByLastMessageAtDesc(userId, userId)
                .stream()
                .mapToLong(conversation -> isOwner(conversation, userId)
                        ? conversation.getOwnerUnreadCount()
                        : conversation.getParticipantUnreadCount())
                .sum();
        return new LostFoundUnreadCountResponse(count);
    }

    public LostFoundUnreadCountResponse markRead(String conversationId, String studentId) {
        String userId = verifiedStudentId(studentId);
        LostFoundConversation conversation = requireConversation(conversationId);
        requireMember(conversation, userId);
        String unreadField = isOwner(conversation, userId)
                ? "ownerUnreadCount"
                : "participantUnreadCount";
        Query query = Query.query(Criteria.where("_id").is(conversationId));
        LostFoundConversation updated = mongoTemplate.findAndModify(
                query,
                new Update().set(unreadField, 0).set("updatedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                LostFoundConversation.class);
        if (updated == null) {
            throw new ResourceNotFoundException("Conversation not found: " + conversationId);
        }
        return unreadCount(studentId);
    }

    private LostFoundConversation createConversation(LostFoundPost post, Student participant) {
        try {
            return conversations.save(LostFoundConversation.builder()
                    .postId(post.getId())
                    .ownerStudentId(post.getOwnerStudentId())
                    .ownerName(post.getOwnerName())
                    .participantStudentId(participant.getStudentId())
                    .participantName(participant.getName())
                    .build());
        } catch (DuplicateKeyException exception) {
            return conversations.findByPostIdAndParticipantStudentId(post.getId(), participant.getStudentId())
                    .orElseThrow(() -> new ConflictException("Could not create conversation", exception));
        }
    }

    private LostFoundMessageResponse saveMessage(
            LostFoundConversation conversation,
            Student sender,
            String content) {
        LostFoundMessage saved = messages.save(LostFoundMessage.builder()
                .conversationId(conversation.getId())
                .senderStudentId(sender.getStudentId())
                .senderName(sender.getName())
                .content(content.trim())
                .build());

        String unreadField = isOwner(conversation, sender.getStudentId())
                ? "participantUnreadCount"
                : "ownerUnreadCount";
        Instant messageTime = saved.getCreatedAt();
        LostFoundConversation updated = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(conversation.getId())),
                new Update()
                        .set("lastMessage", saved.getContent())
                        .set("lastMessageAt", messageTime)
                        .set("updatedAt", messageTime)
                        .inc(unreadField, 1),
                FindAndModifyOptions.options().returnNew(true),
                LostFoundConversation.class);
        if (updated == null) {
            messages.delete(saved);
            throw new ResourceNotFoundException("Conversation not found: " + conversation.getId());
        }
        return messageResponse(saved);
    }

    private LostFoundConversation requireConversation(String conversationId) {
        return conversations.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
    }

    private String verifiedStudentId(String studentId) {
        return students.getByStudentId(studentId.trim()).getStudentId();
    }

    private void requireMember(LostFoundConversation conversation, String studentId) {
        if (!isOwner(conversation, studentId)
                && !conversation.getParticipantStudentId().equals(studentId)) {
            throw new ResourceNotFoundException("Conversation not found: " + conversation.getId());
        }
    }

    private boolean isOwner(LostFoundConversation conversation, String studentId) {
        return conversation.getOwnerStudentId().equals(studentId);
    }

    private LostFoundConversationResponse response(
            LostFoundConversation conversation,
            LostFoundPost post,
            String viewerStudentId) {
        boolean ownerView = isOwner(conversation, viewerStudentId);
        return new LostFoundConversationResponse(
                conversation.getId(),
                post.getId(),
                post.getTitle(),
                post.getType(),
                post.getCategory(),
                ownerView ? conversation.getParticipantStudentId() : conversation.getOwnerStudentId(),
                ownerView ? conversation.getParticipantName() : conversation.getOwnerName(),
                conversation.getLastMessage(),
                conversation.getLastMessageAt(),
                ownerView ? conversation.getOwnerUnreadCount() : conversation.getParticipantUnreadCount(),
                ownerView);
    }

    private LostFoundMessageResponse messageResponse(LostFoundMessage message) {
        return new LostFoundMessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderStudentId(),
                message.getSenderName(),
                message.getContent(),
                message.getCreatedAt());
    }
}
