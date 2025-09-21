package ru.ixlax.hackaton.core;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.ixlax.hackaton.api.publicapi.dto.CameraStatusDto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CameraStatusService {

    private final SimpMessagingTemplate ws;
    private final Map<Long, Boolean> lastOnline = new ConcurrentHashMap<>();

    /** Публикует событие только при смене статуса (чтоб не флудить). */
    public void publish(CameraStatusDto dto) {
        Boolean prev = lastOnline.put(dto.id(), dto.online());
        if (prev == null || prev.booleanValue() != dto.online()) {
            ws.convertAndSend("/topic/camera-status", dto);
        }
    }

    public Map<Long, Boolean> snapshot() {
        return Map.copyOf(lastOnline);
    }
}