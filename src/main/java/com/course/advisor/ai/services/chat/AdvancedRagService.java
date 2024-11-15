package com.course.advisor.ai.services.chat;

import com.course.advisor.ai.utils.PromptUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;


@RequiredArgsConstructor
@Slf4j
@Service
public class AdvancedRagService {
    private static final String CURSES = "curses";

    @Value("${tavily.api-key}")
    private String tavilyApiKey;

    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final QdrantEmbeddingStore.Builder qdrantEmbeddingStoreBuilder;

    public String generateAnswer(ClientChatRequest chatRequest) {
        try {
            QuestionAnsweringAgent agent = advancedQuestionAnsweringAgent();

            String answer = agent.answer(chatRequest.getUserMsg());
            log.info("answer: {}", answer);

            return answer;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "RAG: Exception occurred while generating answer. " + e.getMessage();
        }
    }

    private QuestionAnsweringAgent advancedQuestionAnsweringAgent() throws IOException {
        ContentRetriever embeddingContentRetriever = getEmbeddingStoreContentRetriever();

        ContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(PromptUtil.loadPromptTemplate(this.getClass(), "system_prompt.txt"))
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(embeddingContentRetriever)
                .contentInjector(contentInjector)
                .build();

        return AiServices.builder(QuestionAnsweringAgent.class)
                .chatLanguageModel(openAiChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private ContentRetriever getWebSearchContentRetriever() {
        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
                .apiKey(tavilyApiKey)
                .includeAnswer(true)
                .build();

        return WebSearchContentRetriever.builder()
                .webSearchEngine(webSearchEngine)
                .maxResults(1)
                .build();
    }

    private ContentRetriever getEmbeddingStoreContentRetriever() {
        EmbeddingStore<TextSegment> embeddingStore = qdrantEmbeddingStoreBuilder
                .collectionName(CURSES)
                .build();

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(openAiEmbeddingModel)
                .maxResults(1)
                .minScore(0.75)
                .build();
    }
}
