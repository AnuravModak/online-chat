package com.demo.chatApp.chat;


import com.demo.chatApp.chatroom.ChatRoomService;
import com.demo.chatApp.keyManager.Encryption;
import com.demo.chatApp.keyManager.KeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ChatMessageService {
    private ChatMessageRepository repository;
    private ChatRoomService chatRoomService;
    private KeyRepository keyRepository;
    private Encryption encryption;

    @Autowired
    public ChatMessageService(ChatMessageRepository repository, ChatRoomService chatRoomService){
        this.repository=repository;
        this.chatRoomService=chatRoomService;
    }

    private ChatMessage encryptMessage(ChatMessage chatMessage){
        try {
            Long keyId = keyRepository.findActiveKeyId();
            if (keyId == null) {
                throw new RuntimeException("Failed to save new user as key Id is null");
            }

            chatMessage.setKeyId(keyId);
//            String publicKeyPem = keyRepository.findActivePublicKey(keyId);
//            String privateKeyPem = keyRepository.findActivePrivateKey(keyId); // may not be needed now
//
//            // Convert PEM string to PublicKey
//            PublicKey publicKey = encryption.getPublicKeyFromString(publicKeyPem);
//
//            // Encrypt user fields
//
//            String encryptedChatId=encryption.encryptWithPublicKey(chatMessage.getChatId(), publicKey);
//            String encryptedSenderId = encryption.encryptWithPublicKey(chatMessage.getSenderId(), publicKey);
//            String encryptedRecieverId = encryption.encryptWithPublicKey(chatMessage.getRecipientId(), publicKey);
//            String encryptedContent= encryption.encryptWithPublicKey(chatMessage.getContent(),publicKey);
//
//            // Set encrypted data
//            chatMessage.setChatId(encryptedChatId);
//            chatMessage.setSenderId(encryptedSenderId);
//            chatMessage.setRecipientId(encryptedRecieverId);
//            chatMessage.setContent(encryptedContent);

            return chatMessage;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt user data", e);
        }
    }

    public ChatMessage save(ChatMessage chatMessage) {
        var chatId = chatRoomService
                .getChatRoomId(chatMessage.getSenderId(), chatMessage.getRecipientId(), true)
                .orElseThrow(); // You can create your own dedicated exception
        chatMessage.setChatId(chatId);

        ChatMessage encryptedChatMessage= encryptMessage(chatMessage);
        repository.save(encryptedChatMessage);
        return encryptedChatMessage;
    }

    public List<ChatMessage> findChatMessages(String senderId, String recipientId) {
        var chatId = chatRoomService.getChatRoomId(senderId, recipientId, false);
        return chatId.map(repository::findByChatId).orElse(new ArrayList<>());
    }
}
