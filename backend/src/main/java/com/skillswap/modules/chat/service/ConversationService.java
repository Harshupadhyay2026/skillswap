package com.skillswap.modules.chat.service;

import com.skillswap.exception.BadRequestException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.chat.dto.ConversationResponse;
import com.skillswap.modules.chat.dto.MessageResponse;
import com.skillswap.modules.chat.dto.SendMessageRequest;
import com.skillswap.modules.chat.entity.Conversation;
import com.skillswap.modules.chat.entity.Message;
import com.skillswap.modules.chat.repository.ConversationRepository;
import com.skillswap.modules.chat.repository.MessageRepository;
import com.skillswap.modules.user.dto.PublicUserResponse;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One-to-one messaging.
 *
 * Only the two participants of a conversation can read or write in it. Threads
 * are created lazily and reused, and "read" means "the recipient has opened the
 * thread", which drives the unread badges.
 */
@Service
public class ConversationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /** Get-or-create the thread between the caller and {@code otherUserId}. */
    @Transactional
    public ConversationResponse start(String username, Long otherUserId) {
        User me = getUser(username);
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + otherUserId));

        if (me.getId().equals(other.getId())) {
            throw new BadRequestException("You cannot start a conversation with yourself");
        }
        if (!other.isActive()) {
            throw new BadRequestException("That account is no longer active");
        }

        User one = me.getId() < other.getId() ? me : other;
        User two = me.getId() < other.getId() ? other : me;

        Conversation conversation = conversationRepository
                .findByUserOneIdAndUserTwoId(one.getId(), two.getId())
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setUserOne(one);
                    created.setUserTwo(two);
                    return conversationRepository.save(created);
                });

        Message last = latestMessage(conversation.getId());
        long unread = messageRepository
                .countByConversationIdAndSenderIdNotAndReadAtIsNull(conversation.getId(), me.getId());

        return toResponse(conversation, me, last, unread);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(String username) {
        User me = getUser(username);
        List<Conversation> conversations = conversationRepository.findAllForUser(me.getId());
        if (conversations.isEmpty()) return List.of();

        List<Long> ids = conversations.stream().map(Conversation::getId).toList();

        Map<Long, Message> latest = messageRepository.findLatestForConversations(ids).stream()
                .collect(Collectors.toMap(m -> m.getConversation().getId(), m -> m));

        Map<Long, Long> unread = messageRepository
                .countUnreadByConversationIds(ids, me.getId()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return conversations.stream()
                .map(c -> toResponse(c, me, latest.get(c.getId()),
                        unread.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(String username, Long conversationId) {
        User me = getUser(username);
        Conversation conversation = getParticipant(me, conversationId);

        return toResponse(conversation, me, latestMessage(conversationId),
                messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(
                        conversationId, me.getId()));
    }

    /**
     * Newest page of messages, returned oldest-first.
     *
     * @param beforeId fetch only messages older than this id (paging backwards)
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> messages(String username, Long conversationId,
                                          Long beforeId, int size) {
        User me = getUser(username);
        getParticipant(me, conversationId);

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest page = PageRequest.of(0, safeSize);

        List<Message> batch = beforeId == null
                ? messageRepository.findByConversationIdOrderByIdDesc(conversationId, page)
                : messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(
                        conversationId, beforeId, page);

        Collections.reverse(batch); // oldest → newest for rendering
        return batch.stream().map(MessageResponse::fromEntity).toList();
    }

    @Transactional
    public MessageResponse send(String username, Long conversationId, SendMessageRequest req) {
        User me = getUser(username);
        Conversation conversation = getParticipant(me, conversationId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(me);
        message.setBody(req.getBody().trim());

        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepository.save(conversation);

        return MessageResponse.fromEntity(saved);
    }

    @Transactional
    public int markRead(String username, Long conversationId) {
        User me = getUser(username);
        getParticipant(me, conversationId);
        return messageRepository.markConversationRead(conversationId, me.getId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return messageRepository.countUnreadForUser(getUser(username).getId());
    }

    // ──────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────

    private Conversation getParticipant(User me, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found with id: " + conversationId));

        if (!conversation.includes(me.getId())) {
            // 404 rather than 403 so the existence of a private thread is not leaked.
            throw new ResourceNotFoundException(
                    "Conversation not found with id: " + conversationId);
        }
        return conversation;
    }

    private Message latestMessage(Long conversationId) {
        List<Message> latest = messageRepository.findLatestForConversations(List.of(conversationId));
        return latest.isEmpty() ? null : latest.get(0);
    }

    private ConversationResponse toResponse(Conversation conversation, User me,
                                             Message last, long unread) {
        ConversationResponse res = new ConversationResponse();
        res.setId(conversation.getId());
        res.setCounterpart(PublicUserResponse.fromEntity(conversation.counterpartOf(me.getId())));
        if (last != null) {
            res.setLastMessage(truncate(last.getBody(), 160));
            res.setLastMessageSenderId(last.getSender().getId());
            res.setLastMessageAt(last.getCreatedAt());
        } else {
            res.setLastMessageAt(conversation.getLastMessageAt());
        }
        res.setUnreadCount(unread);
        res.setCreatedAt(conversation.getCreatedAt());
        return res;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private User getUser(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + usernameOrEmail));
    }
}
