package com.bgsistemas.api.notification;

import com.bgsistemas.api.notification.dtos.MailSettingsDTO;
import com.bgsistemas.api.notification.dtos.MailSettingsRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// BGSI-260: "Ajustes > Avisos por correo" of the admin panel (SecurityConfig protects /api/admin/**)
@RestController
@RequestMapping("/api/admin/settings/mail")
public class AdminMailSettingsController {

    private final MailSettingsService mailSettingsService;
    private final NotificationService notificationService;

    public AdminMailSettingsController(MailSettingsService mailSettingsService,
                                       NotificationService notificationService) {
        this.mailSettingsService = mailSettingsService;
        this.notificationService = notificationService;
    }

    // GET /api/admin/settings/mail -> { notifyEmail, senderName, senderAccount }
    @GetMapping
    public MailSettingsDTO getSettings() {
        return MailSettingsDTO.from(mailSettingsService.getSettings());
    }

    // PUT /api/admin/settings/mail { notifyEmail, senderName }
    @PutMapping
    public MailSettingsDTO updateSettings(@Valid @RequestBody MailSettingsRequestDTO request) {
        return MailSettingsDTO.from(mailSettingsService.updateSettings(request.notifyEmail(), request.senderName()));
    }

    // POST /api/admin/settings/mail/test -> 204 if Gmail accepted it, 503 if not
    @PostMapping("/test")
    public ResponseEntity<Void> sendTest() {
        notificationService.sendTest();
        return ResponseEntity.noContent().build();
    }
}
