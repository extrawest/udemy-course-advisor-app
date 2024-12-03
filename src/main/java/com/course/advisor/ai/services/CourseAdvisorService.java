package com.course.advisor.ai.services;

import com.course.advisor.ai.models.CvFileType;
import com.course.advisor.ai.services.agents.DocumentExtractorAgent;
import com.course.advisor.ai.services.data.DataIngestionService;
import com.course.advisor.ai.services.workflow.WorkflowService;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
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
    private final DocumentExtractorAgent documentExtractorAgent;

    public String findCourses(MultipartFile file, CvFileType cvFileType, String requirements) {
        try {
            Base64.getEncoder().encodeToString(file.getBytes());

//            System.out.println(extractContentFromPDF(file));
//            String extractedSummary = documentExtractorAgent.extractSummary(extractContentFromPDF(file));
            System.out.println("---------------------------------------------");
//            System.out.println(extractedSummary);
            String extractContentFromDocs = extractContentFromDocs(file);
            System.out.println(extractContentFromDocs);
            System.out.println("---------------------------------------------");

//            return workflowService.generateResult(cvData, requirements);

            return "";
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

    public String extractContentFromPDF(final MultipartFile multipartFile) {
        String text;

        try (final PDDocument document = PDDocument.load(multipartFile.getInputStream())) {
            final PDFTextStripper pdfStripper = new PDFTextStripper();
            text = pdfStripper.getText(document);
        } catch (final Exception ex) {
            log.error("Error parsing PDF", ex);
            text = "Error parsing PDF";
        }

        return text;
    }

    public String extractContentFromDocs(final MultipartFile multipartFile) throws IOException {
        XWPFDocument xwpfDocument = new XWPFDocument(multipartFile.getInputStream());

        return xwpfDocument.getParagraphs().stream()
                .map(XWPFParagraph::getText)
                .collect(Collectors.joining(" "));
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
