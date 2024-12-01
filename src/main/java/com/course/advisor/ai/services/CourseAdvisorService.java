package com.course.advisor.ai.services;

import com.course.advisor.ai.services.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseAdvisorService {
    private final WorkflowService workflowService;

    public String findCourse(String input) {
        return workflowService.generateResult(input);
    }
}
