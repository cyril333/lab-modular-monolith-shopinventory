package edu.cit.antolijao.notification;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Notification() {}

    public Notification(String message) {
        this.message = message;
    }

    public Long getNotificationId() { return notificationId; }
    public String getMessage() { return message; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}