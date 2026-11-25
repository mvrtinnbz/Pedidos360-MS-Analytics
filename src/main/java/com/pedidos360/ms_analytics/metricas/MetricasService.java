package com.pedidos360.ms_analytics.metricas;

import com.pedidos360.ms_analytics.dynamo.MetricasRepository;
import com.pedidos360.ms_analytics.metricas.MetricasDto.TopProducto;
import com.pedidos360.ms_analytics.metricas.MetricasDto.Totales;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class MetricasService {

    private static final int TOP = 5;

    private final MetricasRepository repo;

    public MetricasService(MetricasRepository repo) {
        this.repo = repo;
    }

    public MetricasDto snapshot() {

        List<Map<String, AttributeValue>> filas = repo.totalesPorProducto();

        long totalVistas = 0;
        long totalAgregados = 0;
        List<TopProducto> vistos = new ArrayList<>();
        List<TopProducto> agregados = new ArrayList<>();

        for (Map<String, AttributeValue> fila : filas) {
            Long id = Long.parseLong(fila.get("productoId").n());
            String nombre = texto(fila, "nombreProducto");
            String categoria = texto(fila, "categoria");
            long v = entero(fila, "vistas");
            long a = entero(fila, "agregados");

            totalVistas += v;
            totalAgregados += a;

            if (v > 0) {
                vistos.add(new TopProducto(id, nombre, categoria, v));
            }
            if (a > 0) {
                agregados.add(new TopProducto(id, nombre, categoria, a));
            }
        }

        return new MetricasDto(
                top(vistos),
                top(agregados),
                new Totales(totalVistas, totalAgregados, totalVistas + totalAgregados, filas.size()),
                Instant.now().toString());
    }

    private static List<TopProducto> top(List<TopProducto> lista) {
        return lista.stream()
                .sorted(Comparator.comparingLong(TopProducto::cantidad).reversed())
                .limit(TOP)
                .toList();
    }

    private static String texto(Map<String, AttributeValue> fila, String clave) {
        AttributeValue v = fila.get(clave);
        return (v == null || v.s() == null) ? "" : v.s();
    }

    private static long entero(Map<String, AttributeValue> fila, String clave) {
        AttributeValue v = fila.get(clave);
        return (v == null || v.n() == null) ? 0 : Long.parseLong(v.n());
    }
}