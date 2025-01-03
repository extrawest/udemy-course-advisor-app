package com.course.advisor.ai.configurations;

import com.course.advisor.ai.utils.PromptUtil;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Value("${ai.chat-model-name}")
    private String chatModelName;
    @Value("${ai.embedding-model-name}")
    private String embeddingModelName;

    @Bean(name = "openAiChatModel")
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

    @Bean(name = "chatOpenAiChatModel")
    public OpenAiChatModel chatOpenAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .timeout(Duration.ofMinutes(2))
                .logRequests(true)
                .logResponses(true)
                .maxRetries(1)
                .temperature(0.0)
                .maxTokens(2000)
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
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

    @Bean(name = "embeddingStoreContentRetriever")
    public ContentRetriever embeddingStoreContentRetriever(OpenAiEmbeddingModel openAiEmbeddingModel, QdrantEmbeddingStore qdrantEmbeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(qdrantEmbeddingStore)
                .embeddingModel(openAiEmbeddingModel)
                .maxResults(1)
                .minScore(0.75)
                .build();
    }

    @Bean(name = "advisorRetrievalAugmentor")
    public RetrievalAugmentor advisorRetrievalAugmentor(@Qualifier("embeddingStoreContentRetriever") ContentRetriever contentRetriever) throws IOException {
        DefaultContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(PromptUtil.loadPromptTemplate(this.getClass(), "recommendation_system_prompt.txt"))
                .build();

        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .build();
    }

    @Bean(name = "extractorRetrievalAugmentor")
    public RetrievalAugmentor extractorRetrievalAugmentor(ContentRetriever contentRetriever) throws IOException {
        DefaultContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(PromptUtil.loadPromptTemplate(this.getClass(), "document_extractor_system_prompt.txt"))
                .build();

        return DefaultRetrievalAugmentor.builder()
                .contentInjector(contentInjector)
                .contentRetriever(contentRetriever)
                .build();
    }
}
