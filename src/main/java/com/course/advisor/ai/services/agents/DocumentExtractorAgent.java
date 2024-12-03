package com.course.advisor.ai.services.agents;

import com.course.advisor.ai.utils.PromptUtil;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentExtractorAgent {
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final OpenAiChatModel openAiChatModel;

    public String extractSummary(String content) {
        try {
            EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(500, 50))
                    .embeddingModel(openAiEmbeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            ingestor.ingest(new Document(content));
            EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(openAiEmbeddingModel)
                    .build();

            DefaultRetrievalAugmentor defaultRetrievalAugmentor = DefaultRetrievalAugmentor.builder()
                    .contentRetriever(contentRetriever)
                    .contentInjector(
                            DefaultContentInjector.builder()
                                    .promptTemplate(PromptUtil.loadPromptTemplate(this.getClass(), "document_extractor_system_prompt.txt"))
                                    .build()
                    ).build();

            ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
                    .chatLanguageModel(openAiChatModel)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .retrievalAugmentor(defaultRetrievalAugmentor)
                    .build();

            return chain.execute("CV summarization");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

}
