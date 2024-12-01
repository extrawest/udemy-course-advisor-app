package com.course.advisor.ai.services.agents;

import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemory = "chatMemory",
        retrievalAugmentor = "recommendationRetrievalAugmentor"
)
public interface CourseRecommendationAgent {

    String answer(String cvDataSummarized);

}