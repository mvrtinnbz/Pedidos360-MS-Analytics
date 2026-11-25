package com.pedidos360.ms_analytics.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    // Crea el topico de recomendaciones si no existe
    @Bean
    public NewTopic topicoRecomendaciones(
            @Value("${analytics.topico-recomendaciones}") String nombre) {
        return TopicBuilder.name(nombre).partitions(1).replicas(1).build();
    }
}