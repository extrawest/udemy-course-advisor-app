package com.course.advisor.ai.configurations;

import com.course.advisor.ai.utils.PromptUtil;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class TogetherAIModelsConfiguration {
    @Value("${ai.api-key}")
    private String apiKey;
    @Value("${ai.base-url}")
    private String baseUrl;
    @Value("${ai.model-name}")
    private String modelName;
    @Value("${ai.embedding-model-name}")
    private String embeddingModelName;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(2))
                .logRequests(true)
                .logResponses(true)
                .maxRetries(1)
                .temperature(0.0)
                .maxTokens(2000)
                .build();

    }

    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .maxRetries(1)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(OpenAiEmbeddingModel openAiEmbeddingModel, QdrantEmbeddingStore qdrantEmbeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(qdrantEmbeddingStore)
                .embeddingModel(openAiEmbeddingModel)
                .maxResults(1)
                .minScore(0.75)
                .build();
    }

    @Bean
    public RetrievalAugmentor retrievalAugmentor(ContentRetriever contentRetriever) throws IOException {
        DefaultContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(PromptUtil.loadPromptTemplate(this.getClass(), "system_prompt.txt"))
                .build();

        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .build();
    }
}
