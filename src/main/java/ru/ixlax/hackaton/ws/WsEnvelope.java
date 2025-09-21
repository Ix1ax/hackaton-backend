// src/main/java/ru/ixlax/hackaton/ws/WsEnvelope.java
package ru.ixlax.hackaton.ws;

public record WsEnvelope(
        String type,   // INCIDENT | NEWS | SENSOR | CAMERA_ALERT | PING
        long ts,
        Object payload
) {}