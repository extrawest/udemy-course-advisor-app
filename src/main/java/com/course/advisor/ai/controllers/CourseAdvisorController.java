package com.course.advisor.ai.controllers;

import com.course.advisor.ai.services.CourseAdvisorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CourseAdvisorController {
    private final CourseAdvisorService service;

    @Operation(summary = "Ready to use")
    @PostMapping( "/findCourse")
    public ResponseEntity<String> findCourse(@RequestParam String input) {
        String result = service.findCourse(input);
        return ResponseEntity.ok().body(result);
    }
}
