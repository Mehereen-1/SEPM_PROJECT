package com.example.project.notification.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.project.notification.service.NotificationService;
import com.example.project.notification.service.NotificationView;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Notification Controller Integration Tests")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("Given notifications when getting notifications then returns list successfully")
    void givenNotifications_whenGettingNotifications_thenReturnsListSuccessfully() throws Exception {
        NotificationView notificationView = new NotificationView(
            11L,
            "Delivery assigned to rider",
            "DELIVERY",
            false,
            OffsetDateTime.parse("2026-04-01T10:00:00+06:00"),
            1711944000000L,
            "Asia/Dhaka"
        );

        when(notificationService.getCurrentUserNotifications(true, 5)).thenReturn(List.of(notificationView));

        mockMvc.perform(get("/notifications")
                .param("unread", "true")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(11))
            .andExpect(jsonPath("$[0].message").value("Delivery assigned to rider"))
            .andExpect(jsonPath("$[0].type").value("DELIVERY"))
            .andExpect(jsonPath("$[0].read").value(false))
            .andExpect(jsonPath("$[0].timestamp").exists());

        verify(notificationService).getCurrentUserNotifications(true, 5);
    }

    @Test
    @DisplayName("Given markable notification when patching by id then returns updated true")
    void givenMarkableNotification_whenPatchingById_thenReturnsUpdatedTrue() throws Exception {
        when(notificationService.markAsRead(21L)).thenReturn(true);

        mockMvc.perform(patch("/notifications/21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updated").value(true));

        verify(notificationService).markAsRead(21L);
    }

    @Test
    @DisplayName("Given notification not found when patching by id then returns not found")
    void givenNotificationNotFound_whenPatchingById_thenReturnsNotFound() throws Exception {
        when(notificationService.markAsRead(404L)).thenReturn(false);

        mockMvc.perform(patch("/notifications/404"))
            .andExpect(status().isNotFound());

        verify(notificationService).markAsRead(404L);
    }

    @Test
    @DisplayName("Given unread notifications when patching all then returns updated count")
    void givenUnreadNotifications_whenPatchingAll_thenReturnsUpdatedCount() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(3);

        mockMvc.perform(patch("/notifications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updatedCount").value(3));

        verify(notificationService).markAllAsRead();
    }
}
