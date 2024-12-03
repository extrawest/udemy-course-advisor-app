package com.course.advisor.ai.services.agents;

import com.course.advisor.ai.utils.PromptUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
@AllArgsConstructor
public class ImageExtractorAgent {
    private final OpenAiChatModel openAiChatModel;

    public String extractSummary(String content) {
        try {
            var systemPrompt = PromptUtil.loadPromptTemplate(this.getClass(), "image_extractor_system.prompt.txt").apply(new HashMap<>());

            var imageContent = ImageContent.from(content, "image/png", ImageContent.DetailLevel.AUTO);
            var textContent = new TextContent(systemPrompt.text());
            var message = UserMessage.from(textContent, imageContent);
            Response<AiMessage> response = openAiChatModel.generate(message);

            return response.content().text();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

}
