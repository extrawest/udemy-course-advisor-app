package com.course.advisor.ai.services;

import com.course.advisor.ai.services.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseAdvisorService {
    private final WorkflowService workflowService;

    public String findCourses(MultipartFile file) {
        try {
            return workflowService.generateResult(Base64.getEncoder().encodeToString(file.getBytes()));
        } catch (Exception e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }
}
