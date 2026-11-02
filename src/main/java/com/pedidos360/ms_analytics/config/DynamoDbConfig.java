package com.pedidos360.ms_analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(@Value("${aws.region}") String region,
                                         @Value("${aws.dynamodb.endpoint:}") String endpoint) {

        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            // LOCAL: DynamoDB Local en Docker (acepta cualquier credencial)
            builder.endpointOverride(URI.create(endpoint))
                   .credentialsProvider(StaticCredentialsProvider.create(
                           AwsBasicCredentials.create("local", "local")));
        }
        // AWS: sin endpoint -> DynamoDB real, credenciales del rol de la EC2
        // (LabInstanceProfile) mediante la cadena de credenciales por defecto.

        return builder.build();
    }
}