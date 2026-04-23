package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.Notification;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping()
    public String showUserNotificationList(
            Model model,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        model.addAttribute("notifications", notificationService.getAll(currentUser.getId(), pageable));
        return "notifications/listNotifications";
    }

    @PostMapping("/{notificationId}")
    public String showNotification(@PathVariable("notificationId") int notificationId, @AuthenticationPrincipal CustomUserDetails currentUser) {
        Notification notification = notificationService.getNotificationById(notificationId);
        notificationService.markAsRead(notificationId, currentUser.getId());
        return "redirect:" + notification.getUrl();
    }

    @PostMapping("/markAllAsRead")
    public String processMarkAllAsRead(@AuthenticationPrincipal CustomUserDetails currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return "redirect:/notifications";
    }
}
