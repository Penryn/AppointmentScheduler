package com.example.slabiak.appointmentscheduler.dao;

import com.example.slabiak.appointmentscheduler.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    @Query("select N from Notification N join N.user u where u.id = :userId and N.isRead=false")
    List<Notification> getAllUnreadNotifications(@Param("userId") int userId);

    @Query("select count(n) from Notification n where n.user.id = :userId and n.isRead=false")
    long countUnreadByUserId(@Param("userId") int userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user.id = :userId and n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") int userId);

    @Query("select n from Notification n where n.user.id = :userId order by n.createdAt desc")
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") int userId);
}
