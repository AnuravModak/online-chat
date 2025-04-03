package com.demo.chatApp.user;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class UserController {
    private UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    @Autowired
    public UserController(UserService userService, SimpMessagingTemplate messagingTemplate){
        this.userService=userService;
        this.messagingTemplate=messagingTemplate;
    }


    @MessageMapping("/user/addUser")
    @SendTo("/user/public")
    public AppUser addUser(
            @Payload AppUser user
    ) {
        userService.saveUser(user);
        return user;
    }

    @MessageMapping("/user/disconnectUser")
    @SendTo("/user/public")
    public AppUser disconnectUser(
            @Payload AppUser user
    ) {
        userService.disconnect(user);
        return user;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AppUser>> findConnectedUsers() {
        return ResponseEntity.ok(userService.findConnectedUsers());
    }

    @MessageMapping("/onlineUser")
    @SendTo("/topic/onlineUsers")
    public Set<String> handleUserOnline (Message<String> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        String payload = message.getPayload();
        System.out.println("Received Payload: " + payload);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(payload);
            String username = jsonNode.get("fullName").asText();



            // ✅ Ensure session attributes exist
            if (accessor.getSessionAttributes() == null) {
                accessor.setSessionAttributes(new HashMap<>());
            }
            accessor.getSessionAttributes().put("fullName", username);

            onlineUsers.add(username);
            return onlineUsers;

        } catch (Exception e) {
            System.err.println("❌ Failed to process user login: " + e.getMessage());
            return onlineUsers; // Ensure return even in case of failure
        }
    }

    @MessageMapping("/typingStatus")
    public void sendTypingStatus(@Payload Map<String, String> typingMessage) {
        String sender = typingMessage.get("sender"); // Get username from the payload
        String recipient = typingMessage.get("recipientId");
        messagingTemplate.convertAndSendToUser(recipient, "/queue/typing", typingMessage);
    }

    @MessageMapping("/offlineUser")
    @SendTo("/topic/onlineUsers")
    public Set<String> handleUserOffline(Message<String> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String payload = message.getPayload();

        System.out.println("Received Payload on offline end: " + payload);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(payload);
            String username = jsonNode.get("fullName").asText();

            // Remove user from the active users set
            onlineUsers.remove(username);

            // Notify all clients about the updated online user list
//            messagingTemplate.convertAndSend("/topic/onlineUsers", onlineUsers);

        } catch (Exception e) {
            System.err.println("❌ Failed to process user logout: " + e.getMessage());
        }

        return onlineUsers;}
}
