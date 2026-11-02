package com.pedidos360.ms_analytics.dynamo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.ArrayList;
import java.util.List;

// Crea las tablas de DynamoDB al arrancar, si no existen.
@Component
public class TablasInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TablasInitializer.class);

    private final DynamoDbClient dynamo;
    private final String tablaInteracciones;
    private final String tablaProductos;

    public TablasInitializer(DynamoDbClient dynamo,
                             @Value("${analytics.tabla-interacciones}") String tablaInteracciones,
                             @Value("${analytics.tabla-productos}") String tablaProductos) {
        this.dynamo = dynamo;
        this.tablaInteracciones = tablaInteracciones;
        this.tablaProductos = tablaProductos;
    }

    @Override
    public void run(ApplicationArguments args) {

        List<String> existentes = dynamo.listTables().tableNames();

        // Interacciones por usuario: PK usuarioId (texto) + SK productoId (numero)
        if (!existentes.contains(tablaInteracciones)) {
            crearTabla(tablaInteracciones,
                    "usuarioId", ScalarAttributeType.S,
                    "productoId", ScalarAttributeType.N);
        }

        // Totales por producto: PK productoId (numero)
        if (!existentes.contains(tablaProductos)) {
            crearTabla(tablaProductos,
                    "productoId", ScalarAttributeType.N,
                    null, null);
        }

        log.info("DynamoDB listo. Tablas: {}", dynamo.listTables().tableNames());
    }

    private void crearTabla(String nombre,
                            String pk, ScalarAttributeType tipoPk,
                            String sk, ScalarAttributeType tipoSk) {

        List<AttributeDefinition> atributos = new ArrayList<>();
        List<KeySchemaElement> claves = new ArrayList<>();

        atributos.add(AttributeDefinition.builder().attributeName(pk).attributeType(tipoPk).build());
        claves.add(KeySchemaElement.builder().attributeName(pk).keyType(KeyType.HASH).build());

        if (sk != null) {
            atributos.add(AttributeDefinition.builder().attributeName(sk).attributeType(tipoSk).build());
            claves.add(KeySchemaElement.builder().attributeName(sk).keyType(KeyType.RANGE).build());
        }

        dynamo.createTable(CreateTableRequest.builder()
                .tableName(nombre)
                .attributeDefinitions(atributos)
                .keySchema(claves)
                .billingMode(BillingMode.PAY_PER_REQUEST)   // sin capacidad fija: se paga por uso
                .build());

        // Espera a que la tabla quede ACTIVE
        dynamo.waiter().waitUntilTableExists(r -> r.tableName(nombre));

        log.info("Tabla DynamoDB creada: {}", nombre);
    }
}