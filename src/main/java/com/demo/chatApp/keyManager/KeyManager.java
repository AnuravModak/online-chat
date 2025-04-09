package com.demo.chatApp.keyManager;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.*;
import java.time.LocalDateTime;
import java.util.Base64;

@Component
public class KeyManager {

    public KeyPair keyPair;

    private final KeyRepository keyRepository;

    @Autowired
    public KeyManager(KeyRepository keyRepository){
        this.keyRepository=keyRepository;
    }


    public KeyPair generateKeyPair() {
        try {
            // 1. Generate new key pair
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair newKeyPair = generator.generateKeyPair();

            // 2. Expire old active key (if exists)
            Long existingKeyId = keyRepository.findActiveKeyId();

            if (existingKeyId != null) {
                keyRepository.expireKeyById(existingKeyId);
            }

            // 3. Convert keys to Base64 strings for storing in DB
            String publicKeyStr = Base64.getEncoder().encodeToString(newKeyPair.getPublic().getEncoded());
            String privateKeyStr = Base64.getEncoder().encodeToString(newKeyPair.getPrivate().getEncoded());

            KeyEntity newKeyEntity = new KeyEntity();
            newKeyEntity.setId(System.currentTimeMillis());
            newKeyEntity.setPublicKey(publicKeyStr);
            newKeyEntity.setPrivateKey(privateKeyStr);
            newKeyEntity.setActive(true);
            newKeyEntity.setCreatedAt(LocalDateTime.now());
            newKeyEntity.setExpiresAt(null);

            keyRepository.save(newKeyEntity);

            return newKeyPair;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate and save RSA key pair: ", e);
        }
    }


    public PublicKey getPublicKey(){
        return this.keyPair.getPublic();
    }

    public PrivateKey getPrivateKey(){
        return this.keyPair.getPrivate();
    }
}
