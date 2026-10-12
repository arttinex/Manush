package com.manush.jogajog.controller;

import com.manush.auth.model.User;
import com.manush.jogajog.model.Message;
import com.manush.jogajog.service.JogajogService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final JogajogService jogajogService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/conversation")
    public Message sendMessage(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        User user = new User();
        user.setId(userId);
        
        return jogajogService.sendMessage(message.getConversationId(), user, message.getText());
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/conversation")
    public Message addUser(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("userId", message.getSenderId());
        return message;
    }
}
