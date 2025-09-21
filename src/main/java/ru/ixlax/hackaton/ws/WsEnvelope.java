// src/main/java/ru/ixlax/hackaton/ws/WsEnvelope.java
package ru.ixlax.hackaton.ws;

public record WsEnvelope(
        String type,
        long ts,
        Object payload
) {}