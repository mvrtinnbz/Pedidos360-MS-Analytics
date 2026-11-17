package com.pedidos360.ms_analytics.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Evento que publican ms-productos y ms-carrito en el topico tienda.eventos.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TiendaEvento(
        String tipo,            // PRODUCTO_VISTO | PRODUCTO_AGREGADO_CARRITO
        String usuarioId,       // correo del usuario o "anonimo"
        Long productoId,
        String nombreProducto,
        String categoria,
        String origen,          // ms-productos | ms-carrito
        String fecha
) {
    public static final String PRODUCTO_VISTO = "PRODUCTO_VISTO";
    public static final String PRODUCTO_AGREGADO_CARRITO = "PRODUCTO_AGREGADO_CARRITO";
    public static final String ANONIMO = "anonimo";
}