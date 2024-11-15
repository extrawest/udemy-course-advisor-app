package com.course.advisor.ai.controllers;

import com.course.advisor.ai.services.CourseAdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CourseAdvisorController {
    private final CourseAdvisorService service;

}
