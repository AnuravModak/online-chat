package com.demo.chatApp.keyManager;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface KeyRepository extends JpaRepository<KeyEntity,Long> {


    @Query(value = "SELECT id FROM master_keys WHERE active = true LIMIT 1", nativeQuery = true)
    public Long findActiveKeyId();

    @Modifying
    @Query(value = "UPDATE master_keys SET expires_at = CURRENT_TIMESTAMP, active=false WHERE id = :id", nativeQuery = true)
    void expireKeyById(@Param("id") Long id);


}
