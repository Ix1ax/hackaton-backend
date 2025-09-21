package ru.ixlax.hackaton.api.p2p.dto;

public record EventEnvelope(
        String type,
        String nodeId,
        long ts,
        Object payload,
        String signature
) {}