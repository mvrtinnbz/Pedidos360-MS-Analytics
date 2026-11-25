package com.pedidos360.ms_analytics.metricas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MetricasNotificador {

    private final AtomicBoolean pendiente = new AtomicBoolean(false);
    private final MetricasWebSocketHandler handler;

    public MetricasNotificador(MetricasWebSocketHandler handler) {
        this.handler = handler;
    }

    public void marcarCambio() {
        pendiente.set(true);
    }

    @Scheduled(fixedDelay = 1000)
    public void emitirSiHayCambios() {
        if (pendiente.getAndSet(false)) {
            handler.emitirATodos();
        }
    }
}