package com.pedidos360.ms_analytics.metricas;

import java.util.List;

public record MetricasDto(
        List<TopProducto> masVistos,
        List<TopProducto> masAgregados,
        Totales totales,
        String actualizado
) {
    public record TopProducto(Long productoId, String nombre, String categoria, long cantidad) {}

    public record Totales(long vistas, long agregados, long interacciones, int productosConActividad) {}
}