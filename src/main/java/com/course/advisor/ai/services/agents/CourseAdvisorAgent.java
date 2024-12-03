package com.course.advisor.ai.services.agents;

import com.course.advisor.ai.utils.PromptUtil;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CourseAdvisorAgent {
    private final OpenAiChatModel chatOpenAiChatModel;
    private final ChatMemory chatMemory;
    private final ContentRetriever embeddingStoreContentRetriever;

    public String generateAdvice(String cvDataSummarized) {
        try {
            DefaultRetrievalAugmentor defaultRetrievalAugmentor = DefaultRetrievalAugmentor.builder()
                    .contentRetriever(embeddingStoreContentRetriever)
                    .contentInjector(
                            DefaultContentInjector.builder()
                                    .promptTemplate(PromptUtil.loadPromptTemplate(this.getClass(), "recommendation_system_prompt.txt"))
                                    .build()
                    ).build();

            ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
                    .chatLanguageModel(chatOpenAiChatModel)
                    .chatMemory(chatMemory)
                    .retrievalAugmentor(defaultRetrievalAugmentor)
                    .build();

            return chain.execute(cvDataSummarized);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

}