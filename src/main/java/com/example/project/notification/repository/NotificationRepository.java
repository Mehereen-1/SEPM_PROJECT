package com.example.project.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.project.notification.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    List<Notification> findByRecipient_IdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipient_IdAndIsReadFalse(Long recipientId);

    Optional<Notification> findByIdAndRecipient_Id(Long id, Long recipientId);

    boolean existsByEventKeyAndRecipient_Id(String eventKey, Long recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.recipient.id = :recipientId and n.isRead = false")
    int markAllReadByRecipientId(@Param("recipientId") Long recipientId);
}
