package com.course.advisor.ai.configurations;

import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfiguration {
    public static final String UDEMY_CURSES = "udemy_curses";

    @Value("${qgrant.api-key}")
    private String qgrantApiKey;
    @Value("${qgrant.grpc-host}")
    private String qgrantGrpcHost;

    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(qgrantGrpcHost, 6334, true)
                        .withApiKey(qgrantApiKey)
                        .build()
        );
    }

    @Bean
    public QdrantEmbeddingStore qdrantEmbeddingStoreBuilder() {
        return QdrantEmbeddingStore.builder()
                .host(qgrantGrpcHost)
                .port(6334)
                .apiKey(qgrantApiKey)
                .collectionName(UDEMY_CURSES)
                .useTls(true)
                .build();
    }
}
