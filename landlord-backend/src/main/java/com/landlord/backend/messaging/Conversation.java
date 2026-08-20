package com.landlord.backend.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String withName;

    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getWithName() {
        return withName;
    }

    public void setWithName(String withName) {
        this.withName = withName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
