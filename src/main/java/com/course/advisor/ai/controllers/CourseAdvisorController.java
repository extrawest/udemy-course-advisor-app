package com.course.advisor.ai.controllers;

import com.course.advisor.ai.models.CourseAdviseResponse;
import com.course.advisor.ai.services.CourseAdvisorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
public class CourseAdvisorController {
    private final CourseAdvisorService service;

    @Operation(summary = "Ready to use")
    @PostMapping(
            path = "/findCourses",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CourseAdviseResponse> findCourses(
            @RequestPart MultipartFile file,
            @RequestParam(required = false) String requirements
    ) {
        String result = service.findCourses(file, requirements);
        return ResponseEntity.ok().body(new CourseAdviseResponse(result));
    }

    @Operation(summary = "Ready to use")
    @PostMapping(
            path = "/initDB",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CourseAdviseResponse> initDB(@RequestPart MultipartFile file) {
        service.initDB(file);
        return ResponseEntity.ok().body(new CourseAdviseResponse("Check logs"));
    }
}
