package com.pedidos360.ms_analytics.recomendaciones;

import com.pedidos360.ms_analytics.dynamo.MetricasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

@Service
public class RecomendacionService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionService.class);
    private static final int MAXIMO = 4;

    private final MetricasRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper jsonMapper;
    private final RestClient productosClient;
    private final String topico;

    public RecomendacionService(MetricasRepository repo,
                                KafkaTemplate<String, String> kafka,
                                JsonMapper jsonMapper,
                                @Value("${productos.api.url}") String productosUrl,
                                @Value("${analytics.topico-recomendaciones}") String topico) {
        this.repo = repo;
        this.kafka = kafka;
        this.jsonMapper = jsonMapper;
        this.productosClient = RestClient.create(productosUrl);
        this.topico = topico;
    }

    public List<ProductoDto> recomendarPara(String usuarioId) {

        List<ProductoDto> catalogo = obtenerCatalogo();

        // Popularidad global: vistas + 3 * agregados por producto
        Map<Long, Double> popularidad = new HashMap<>();
        for (Map<String, AttributeValue> fila : repo.totalesPorProducto()) {
            popularidad.put(numeroL(fila, "productoId"), puntaje(fila));
        }

        // Preferencias del usuario: puntaje por categoria y productos ya vistos
        Map<String, Double> porCategoria = new HashMap<>();
        Set<Long> vistos = new HashSet<>();

        if (usuarioId != null && !usuarioId.isBlank()) {
            for (Map<String, AttributeValue> fila : repo.interaccionesDeUsuario(usuarioId)) {
                vistos.add(numeroL(fila, "productoId"));
                AttributeValue cat = fila.get("categoria");
                if (cat != null && cat.s() != null) {
                    porCategoria.merge(cat.s(), puntaje(fila), Double::sum);
                }
            }
        }

        // 1) Productos no vistos de las categorias favoritas del usuario
        List<ProductoDto> resultado = new ArrayList<>(catalogo.stream()
                .filter(p -> !vistos.contains(p.id()))
                .filter(p -> porCategoria.containsKey(p.categoria()))
                .sorted(Comparator
                        .comparingDouble((ProductoDto p) -> porCategoria.get(p.categoria())).reversed()
                        .thenComparing(p -> -popularidad.getOrDefault(p.id(), 0.0)))
                .limit(MAXIMO)
                .toList());

        // 2) Si faltan, se completa con los mas populares (sin repetir)
        if (resultado.size() < MAXIMO) {
            Set<Long> ya = new HashSet<>();
            resultado.forEach(p -> ya.add(p.id()));

            catalogo.stream()
                    .filter(p -> !ya.contains(p.id()))
                    .sorted(Comparator.comparingDouble(
                            (ProductoDto p) -> popularidad.getOrDefault(p.id(), 0.0)).reversed())
                    .limit(MAXIMO - resultado.size())
                    .forEach(resultado::add);
        }

        publicar(usuarioId, resultado);
        return resultado;
    }

    private List<ProductoDto> obtenerCatalogo() {
        ProductoDto[] lista = productosClient.get()
                .uri("/api/productos")
                .retrieve()
                .body(ProductoDto[].class);
        return lista == null ? List.of() : List.of(lista);
    }

    // Publica el resultado en tienda.recomendaciones (si Kafka falla, no se cae la API)
    private void publicar(String usuarioId, List<ProductoDto> recomendados) {
        try {
            String json = jsonMapper.writeValueAsString(Map.of(
                    "usuarioId", usuarioId == null ? "anonimo" : usuarioId,
                    "productoIds", recomendados.stream().map(ProductoDto::id).toList(),
                    "fecha", java.time.Instant.now().toString()));
            kafka.send(topico, usuarioId == null ? "anonimo" : usuarioId, json);
        } catch (Exception e) {
            log.warn("No se pudo publicar en {}: {}", topico, e.getMessage());
        }
    }

    private static double puntaje(Map<String, AttributeValue> fila) {
        return numeroD(fila, "vistas") + 3 * numeroD(fila, "agregados");
    }

    private static double numeroD(Map<String, AttributeValue> fila, String clave) {
        AttributeValue v = fila.get(clave);
        return (v == null || v.n() == null) ? 0 : Double.parseDouble(v.n());
    }

    private static Long numeroL(Map<String, AttributeValue> fila, String clave) {
        return Long.parseLong(fila.get(clave).n());
    }
}