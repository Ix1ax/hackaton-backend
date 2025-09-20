package ru.ixlax.hackaton.api.p2p.dto;

public record EventEnvelope(
        String type,      // "INCIDENT" | "NEWS" (пока INCIDENT)
        String nodeId,
        long ts,
        Object payload,   // List<IncidentDto>
        String signature  // можно оставить null, позже HMAC
) {}