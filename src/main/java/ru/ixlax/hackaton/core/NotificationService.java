package ru.ixlax.hackaton.core;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.ixlax.hackaton.domain.entity.Incident;

@Service
@RequiredArgsConstructor
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mail;

    @Value("${alerts.email.to:}")
    private String to;

    public void incidentRaised(Incident e) {
        if (mail == null) return;
        if (to == null || to.isBlank()) return;

        var m = new SimpleMailMessage();
        m.setTo(to);
        m.setSubject("[ALERT] " + e.getKind() + " " + e.getLevel());
        m.setText(
                "Регион: " + e.getRegionCode() + "\n" +
                        "Координаты: " + e.getLat() + "," + e.getLng() + "\n" +
                        "Incident: " + e.getExternalId()
        );
        try { mail.send(m); } catch (Exception ignore) {}
    }
}