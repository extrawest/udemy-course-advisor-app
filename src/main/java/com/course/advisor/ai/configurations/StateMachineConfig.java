package com.course.advisor.ai.configurations;

import com.course.advisor.ai.models.CvFileType;
import com.course.advisor.ai.services.agents.DocumentExtractorAgent;
import com.course.advisor.ai.services.agents.ImageExtractorAgent;
import com.course.advisor.ai.services.agents.CourseRecommendationAgent;
import com.course.advisor.ai.services.workflow.Events;
import com.course.advisor.ai.services.workflow.States;
import com.course.advisor.ai.services.workflow.Variables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
@EnableStateMachineFactory
@Slf4j
class StateMachineConfig extends StateMachineConfigurerAdapter<States, Events> {
    private final CourseRecommendationAgent courseRecommendationAgent;
    private final DocumentExtractorAgent documentExtractorAgent;
    private final ImageExtractorAgent imageExtractorAgent;

    StateMachineConfig(
            CourseRecommendationAgent courseRecommendationAgent,
            ImageExtractorAgent imageExtractorAgent,
            DocumentExtractorAgent documentExtractorAgent
    ) {
        this.courseRecommendationAgent = courseRecommendationAgent;
        this.imageExtractorAgent = imageExtractorAgent;
        this.documentExtractorAgent = documentExtractorAgent;
    }

    @Override
    public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
        states
                .withStates()
                .initial(States.AWAITING_INPUT)
                .state(States.CS_DATA_EXTRACTION, extractCvData())
                .state(States.RESULT_GENERATION, generateResult())
                .end(States.SUCCESSFUL_COMPLETION)
                .end(States.FAILED_COMPLETION);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
        transitions
                .withExternal().source(States.AWAITING_INPUT).target(States.CS_DATA_EXTRACTION).event(Events.INPUT_RECEIVED).and()
                .withExternal().source(States.CS_DATA_EXTRACTION).target(States.RESULT_GENERATION).event(Events.COURSE_FOUND).and()
                .withExternal().source(States.CS_DATA_EXTRACTION).target(States.FAILED_COMPLETION).event(Events.COURSE_NOT_FOUND).and()
                .withExternal().source(States.RESULT_GENERATION).target(States.SUCCESSFUL_COMPLETION).event(Events.RESULT_VERIFIED).and()
                .withExternal().source(States.RESULT_GENERATION).target(States.FAILED_COMPLETION).event(Events.RESULT_REJECTED);
    }

    private Action<States, Events> extractCvData() {
        return stateContext -> {
            log.info("Extracting CV data...");
            var cvFileType = CvFileType.valueOf(getVariable(stateContext, Variables.CV_FILE_TYPE));
            var input = getVariable(stateContext, Variables.INPUT);

            log.info("Input {}", input);

            String extractedSummary;
            if (cvFileType.equals(CvFileType.PDF) || cvFileType.equals(CvFileType.DOCS)) {
                extractedSummary = documentExtractorAgent.extractSummary(input);
            } else {
                extractedSummary = imageExtractorAgent.extractSummary(input);
            }

            if (Objects.nonNull(extractedSummary)) {
                log.info("CV data summarized {}", extractedSummary);
                stateContext.getExtendedState().getVariables().put(Variables.CV_DATA, extractedSummary);
                sendEvent(stateContext.getStateMachine(), Events.COURSE_FOUND);
            } else {
                sendEvent(stateContext.getStateMachine(), Events.COURSE_NOT_FOUND);
            }
        };
    }

    private Action<States, Events> generateResult() {
        return stateContext -> {
            log.info("Generating result...");
            var cvDataSummarized = getVariable(stateContext, Variables.CV_DATA);
            var requirements = getVariable(stateContext, Variables.REQUIREMENTS);

            String question = cvDataSummarized + requirements;

            log.info("Question {}", question);
            var result = courseRecommendationAgent.answer(question);

            if (Objects.nonNull(result)) {
                stateContext.getExtendedState().getVariables().put(Variables.RESULT, result);
                sendEvent(stateContext.getStateMachine(), Events.RESULT_VERIFIED);
            } else {
                sendEvent(stateContext.getStateMachine(), Events.RESULT_REJECTED);
            }
        };
    }

    private String getVariable(StateContext<States, Events> stateContext, String key) {
        Object variable = stateContext.getExtendedState().getVariables().get(key);
        if (Objects.nonNull(variable)) {
            return variable.toString();
        } else {
            return null;
        }
    }

    private void sendEvent(StateMachine<States, Events> stateMachine, Events event) {
        var message = MessageBuilder.withPayload(event).build();
        stateMachine.sendEvent(Mono.just(message)).subscribe();
    }
}
