package com.course.advisor.ai.services.data;

import com.course.advisor.ai.configurations.QdrantConfiguration;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Slf4j
@Service
public class DataIngestionService {
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final QdrantEmbeddingStore qdrantEmbeddingStore;
    private final QdrantClient qdrantClient;

    public void createCollection() {
        try {
            qdrantClient.createCollectionAsync(
                    QdrantConfiguration.UDEMY_CURSES,
                    VectorParams.newBuilder().setDistance(Distance.Dot).setSize(768).build()
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public void insertDocuments(String content) {
        try {
            DocumentSplitter documentSplitter = DocumentSplitters.recursive(1000, 150);
            Document doc = Document.from(content, Metadata.from("document-type", "history-document"));

            List<TextSegment> segments = documentSplitter.split(doc);

            Response<List<Embedding>> embeddingResponse = openAiEmbeddingModel.embedAll(segments);
            List<Embedding> embeddings = embeddingResponse.content();
            qdrantEmbeddingStore.addAll(embeddings, segments);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
