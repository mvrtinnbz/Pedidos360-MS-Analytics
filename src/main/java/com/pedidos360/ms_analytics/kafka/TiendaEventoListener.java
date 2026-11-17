package com.pedidos360.ms_analytics.kafka;

import com.pedidos360.ms_analytics.dynamo.MetricasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

// Consume el topico tienda.eventos (grupo "ms-analytics") y actualiza DynamoDB.
@Component
public class TiendaEventoListener {

    private static final Logger log = LoggerFactory.getLogger(TiendaEventoListener.class);

    public static final String TOPICO = "tienda.eventos";

    private final JsonMapper jsonMapper;
    private final MetricasRepository metricasRepository;

    public TiendaEventoListener(JsonMapper jsonMapper, MetricasRepository metricasRepository) {
        this.jsonMapper = jsonMapper;
        this.metricasRepository = metricasRepository;
    }

    @KafkaListener(topics = TOPICO)
    public void recibir(String mensaje) {

        // try/catch: un mensaje malo se omite, no detiene el consumidor
        try {
            TiendaEvento evento = jsonMapper.readValue(mensaje, TiendaEvento.class);

            if (!esEventoValido(evento)) {
                log.info("Mensaje ignorado (no es un evento de la tienda): {}", mensaje);
                return;
            }

            metricasRepository.registrar(evento);

            log.info("Kafka -> DynamoDB: {} producto {} (usuario {})",
                    evento.tipo(), evento.productoId(), evento.usuarioId());

        } catch (Exception e) {
            log.warn("No se pudo procesar el mensaje [{}]: {}", mensaje, e.getMessage());
        }
    }

    private static boolean esEventoValido(TiendaEvento evento) {
        return evento != null
                && evento.productoId() != null
                && (TiendaEvento.PRODUCTO_VISTO.equals(evento.tipo())
                    || TiendaEvento.PRODUCTO_AGREGADO_CARRITO.equals(evento.tipo()));
    }
}