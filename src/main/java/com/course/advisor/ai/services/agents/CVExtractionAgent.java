package com.course.advisor.ai.services.agents;

import dev.langchain4j.service.spring.AiService;

//@AiService
public interface CVExtractionAgent {

    String answer(String cvData);

}
