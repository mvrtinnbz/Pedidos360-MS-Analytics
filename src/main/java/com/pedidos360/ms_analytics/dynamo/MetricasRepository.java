package com.pedidos360.ms_analytics.dynamo;

import com.pedidos360.ms_analytics.kafka.TiendaEvento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

// Acceso a DynamoDB: contadores atomicos (ADD) por producto y por usuario+producto.
@Repository
public class MetricasRepository {

    private final DynamoDbClient dynamo;
    private final String tablaInteracciones;
    private final String tablaProductos;

    public MetricasRepository(DynamoDbClient dynamo,
                              @Value("${analytics.tabla-interacciones}") String tablaInteracciones,
                              @Value("${analytics.tabla-productos}") String tablaProductos) {
        this.dynamo = dynamo;
        this.tablaInteracciones = tablaInteracciones;
        this.tablaProductos = tablaProductos;
    }

    // Suma 1 al contador que corresponde al tipo de evento.
    public void registrar(TiendaEvento evento) {

        String contador = TiendaEvento.PRODUCTO_VISTO.equals(evento.tipo()) ? "vistas" : "agregados";

        // 1. Totales del producto (cuenta tambien a los anonimos)
        Map<String, AttributeValue> claveProducto = Map.of(
                "productoId", numero(evento.productoId()));

        incrementar(tablaProductos, claveProducto, contador, evento);

        // 2. Interaccion del usuario con el producto (solo usuarios con sesion)
        if (evento.usuarioId() != null && !TiendaEvento.ANONIMO.equals(evento.usuarioId())) {

            Map<String, AttributeValue> claveInteraccion = Map.of(
                    "usuarioId", AttributeValue.fromS(evento.usuarioId()),
                    "productoId", numero(evento.productoId()));

            incrementar(tablaInteracciones, claveInteraccion, contador, evento);
        }
    }

    // UpdateItem: ADD contador 1 + SET datos descriptivos (si vienen)
    private void incrementar(String tabla, Map<String, AttributeValue> clave,
                             String contador, TiendaEvento evento) {

        Map<String, String> nombres = new HashMap<>();
        Map<String, AttributeValue> valores = new HashMap<>();

        nombres.put("#c", contador);
        valores.put(":uno", AttributeValue.fromN("1"));

        StringBuilder set = new StringBuilder();
        agregarSet(set, nombres, valores, "nombreProducto", evento.nombreProducto());
        agregarSet(set, nombres, valores, "categoria", evento.categoria());
        agregarSet(set, nombres, valores, "ultimaInteraccion", evento.fecha());

        String expresion = "ADD #c :uno" + (set.isEmpty() ? "" : " SET " + set);

        dynamo.updateItem(UpdateItemRequest.builder()
                .tableName(tabla)
                .key(clave)
                .updateExpression(expresion)
                .expressionAttributeNames(nombres)
                .expressionAttributeValues(valores)
                .build());
    }

    private static void agregarSet(StringBuilder set, Map<String, String> nombres,
                                   Map<String, AttributeValue> valores,
                                   String atributo, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (!set.isEmpty()) {
            set.append(", ");
        }
        set.append("#").append(atributo).append(" = :").append(atributo);
        nombres.put("#" + atributo, atributo);
        valores.put(":" + atributo, AttributeValue.fromS(valor));
    }

    private static AttributeValue numero(Long valor) {
        return AttributeValue.fromN(String.valueOf(valor));
    }

    // Interacciones de un usuario (consulta por clave de particion usuarioId)
    public List<Map<String, AttributeValue>> interaccionesDeUsuario(String usuarioId) {
        return dynamo.query(QueryRequest.builder()
                .tableName(tablaInteracciones)
                .keyConditionExpression("usuarioId = :u")
                .expressionAttributeValues(Map.of(":u", AttributeValue.fromS(usuarioId)))
                .build()).items();
    }

    // Totales de todos los productos (tabla chica: un scan alcanza)
    public List<Map<String, AttributeValue>> totalesPorProducto() {
        return dynamo.scan(ScanRequest.builder()
                .tableName(tablaProductos)
                .build()).items();
    }
}