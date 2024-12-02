package com.course.advisor.ai.services;

import com.course.advisor.ai.services.data.DataIngestionService;
import com.course.advisor.ai.services.workflow.WorkflowService;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseAdvisorService {
    private final WorkflowService workflowService;
    private final DataIngestionService dataIngestionService;

    public String findCourses(MultipartFile file) {
        try {
            return workflowService.generateResult(Base64.getEncoder().encodeToString(file.getBytes()));
        } catch (Exception e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }

    public void initDB(MultipartFile file) {
        try {
            dataIngestionService.createCollection();
            String content = getContent(file);
            dataIngestionService.insertDocuments(content);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private String getContent(MultipartFile file)  {
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
            List<String[]> lines = csvReader.readAll().stream().map(array -> new String[]{array[1], array[2], array[8], array[11]}).toList();

            return lines.stream()
                    .map(array -> Arrays.stream(array).map(String::valueOf).collect(Collectors.joining(" ")))
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
