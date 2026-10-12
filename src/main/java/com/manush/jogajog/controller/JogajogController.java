package com.manush.jogajog.controller;

import com.manush.auth.model.User;
import com.manush.jogajog.model.Conversation;
import com.manush.jogajog.model.Message;
import com.manush.jogajog.service.JogajogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Jogajog", description = "Messaging and conversations")
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class JogajogController {

    private final JogajogService jogajogService;

    @Operation(summary = "Get all conversations for current user")
    @GetMapping
    public ResponseEntity<List<Conversation>> getConversations(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(jogajogService.getConversations(user));
    }

    @Operation(summary = "Search conversations")
    @GetMapping("/search")
    public ResponseEntity<List<Conversation>> searchConversations(
            @AuthenticationPrincipal User user,
            @RequestParam String query) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(jogajogService.searchConversations(user, query));
    }

    @Operation(summary = "Get conversation by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Conversation> getConversation(@PathVariable String id) {
        return ResponseEntity.ok(jogajogService.getConversation(id));
    }

    @Operation(summary = "Create new conversation")
    @PostMapping
    public ResponseEntity<Conversation> createConversation(
            @AuthenticationPrincipal User user,
            @RequestBody CreateConversationRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(jogajogService.createConversation(user, request.participantId));
    }

    @Operation(summary = "Get messages in conversation")
    @GetMapping("/{id}/messages")
    public ResponseEntity<Page<Message>> getMessages(
            @PathVariable String id,
            Pageable pageable) {
        return ResponseEntity.ok(jogajogService.getMessages(id, pageable));
    }

    @Operation(summary = "Send message in conversation")
    @PostMapping("/{id}/messages")
    public ResponseEntity<Message> sendMessage(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @RequestBody SendMessageRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(jogajogService.sendMessage(id, user, request.text));
    }

    @Operation(summary = "Block conversation")
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> blockConversation(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        jogajogService.blockConversation(id, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search users to start conversation")
    @GetMapping("/users/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(jogajogService.searchUsers(query));
    }

    public static class CreateConversationRequest {
        public String participantId;
    }

    public static class SendMessageRequest {
        public String text;
    }
}
