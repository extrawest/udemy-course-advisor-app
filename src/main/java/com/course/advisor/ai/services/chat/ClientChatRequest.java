package com.course.advisor.ai.services.chat;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ClientChatRequest {
    private String userMsg;
}