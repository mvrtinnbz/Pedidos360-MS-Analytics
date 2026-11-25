package com.pedidos360.ms_analytics.metricas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class MetricasWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MetricasWebSocketHandler.class);

    private final Set<WebSocketSession> sesiones = new CopyOnWriteArraySet<>();
    private final MetricasService service;
    private final JsonMapper jsonMapper;

    public MetricasWebSocketHandler(MetricasService service, JsonMapper jsonMapper) {
        this.service = service;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sesiones.add(session);
        log.info("Dashboard conectado ({} sesiones)", sesiones.size());
        try {
            enviar(session, snapshotJson());
        } catch (Exception e) {
            log.warn("No se pudo enviar el estado inicial: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sesiones.remove(session);
        log.info("Dashboard desconectado ({} sesiones)", sesiones.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sesiones.remove(session);
    }

    public void emitirATodos() {
        if (sesiones.isEmpty()) {
            return;
        }
        try {
            String mensaje = snapshotJson();
            sesiones.forEach(s -> enviar(s, mensaje));
        } catch (Exception e) {
            log.warn("No se pudieron emitir las metricas: {}", e.getMessage());
        }
    }

    private void enviar(WebSocketSession session, String mensaje) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(mensaje));
                }
            }
        } catch (Exception e) {
            sesiones.remove(session);
        }
    }

    private String snapshotJson() {
        return jsonMapper.writeValueAsString(service.snapshot());
    }
}