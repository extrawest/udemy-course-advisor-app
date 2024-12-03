package com.course.advisor.ai.services.workflow;

import com.course.advisor.ai.models.CvFileType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowService {
    private final StateMachineFactory<States, Events> factory;

    WorkflowService(StateMachineFactory<States, Events> factory) {
        this.factory = factory;
    }

    public String generateResult(MultipartFile file, CvFileType cvFileType, String requirements) {
        var resultFuture = new CompletableFuture<String>();
        var stateMachine = factory.getStateMachine();
        addStateListener(stateMachine, resultFuture);

        try {
            String input;

            if (cvFileType.equals(CvFileType.PNG)) {
                input = Base64.getEncoder().encodeToString(file.getBytes());
            } else if (cvFileType.equals(CvFileType.PDF)) {
                input = extractContentFromPDF(file);
            } else {
                input = extractContentFromDocs(file);
            }

            stateMachine.startReactively()
                    .doOnError(resultFuture::completeExceptionally)
                    .subscribe();

            stateMachine.getExtendedState().getVariables().put(Variables.CV_FILE_TYPE, cvFileType.name());
            stateMachine.getExtendedState().getVariables().put(Variables.INPUT, input);
            if (Objects.nonNull(requirements)) {
                stateMachine.getExtendedState().getVariables().put(Variables.REQUIREMENTS, ", details: " + requirements);
            } else {
                stateMachine.getExtendedState().getVariables().put(Variables.REQUIREMENTS, ", details: no additional requirements");
            }

            stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(Events.INPUT_RECEIVED).build())).subscribe();

            return resultFuture.get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("State machine execution failed", e);
            Thread.currentThread().interrupt();
            return e.getMessage();
        } finally {
            stateMachine.stopReactively().block();
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

    private void addStateListener(StateMachine<States, Events> stateMachine, CompletableFuture<String> resultFuture) {
        stateMachine.addStateListener(new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<States, Events> from, State<States, Events> to) {
                if (to != null) {
                    if (to.getId() == States.SUCCESSFUL_COMPLETION) {
                        Object resultObj = stateMachine.getExtendedState().getVariables().get(Variables.RESULT);
                        if (resultObj != null) {
                            resultFuture.complete(resultObj.toString());
                        } else {
                            log.error("Result not found at successful completion");
                            resultFuture.completeExceptionally(new IllegalStateException("Result not found at successful completion state"));
                        }
                    } else if (to.getId() == States.FAILED_COMPLETION) {
                        log.warn("Workflow ended due to invalid requirements.");
                        resultFuture.complete("Invalid requirements: Your input is either unclear or too complex.");
                    }
                }
            }

            @Override
            public void stateMachineError(StateMachine<States, Events> stateMachine, Exception exception) {
                log.error("State machine encountered an error", exception);
                resultFuture.completeExceptionally(exception);
            }
        });
    }
}

