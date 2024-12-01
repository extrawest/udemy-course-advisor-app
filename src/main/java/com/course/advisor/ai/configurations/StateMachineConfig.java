package com.course.advisor.ai.configurations;

import com.course.advisor.ai.services.agents.CVExtractionAgent;
import com.course.advisor.ai.services.agents.CourseRecommendationAgent;
import com.course.advisor.ai.services.workflow.Events;
import com.course.advisor.ai.services.workflow.States;
import com.course.advisor.ai.services.workflow.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class StateMachineConfig extends StateMachineConfigurerAdapter<States, Events> {

    private static final Logger log = LoggerFactory.getLogger(StateMachineConfig.class);

    private final CourseRecommendationAgent courseRecommendationAgent;
    private final CVExtractionAgent cvExtractionAgent;

    StateMachineConfig(CourseRecommendationAgent courseRecommendationAgent, CVExtractionAgent cvExtractionAgent) {
        this.courseRecommendationAgent = courseRecommendationAgent;
        this.cvExtractionAgent = cvExtractionAgent;
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
            var requirements = getVariable(stateContext, Variables.INPUT);
            String cvData = cvExtractionAgent.answer(requirements);

            if (Objects.nonNull(cvData)) {
                stateContext.getExtendedState().getVariables().put(Variables.CV_DATA, cvData);
                sendEvent(stateContext.getStateMachine(), Events.COURSE_FOUND);
            } else {
                sendEvent(stateContext.getStateMachine(), Events.COURSE_NOT_FOUND);
            }
        };
    }

    private Action<States, Events> generateResult() {
        return stateContext -> {
            log.info("Generating result...");
            var cvData = getVariable(stateContext, Variables.CV_DATA);
            var result = courseRecommendationAgent.answer(cvData);

            if (Objects.nonNull(result)) {
                stateContext.getExtendedState().getVariables().put(Variables.RESULT, result);
                sendEvent(stateContext.getStateMachine(), Events.RESULT_VERIFIED);
            } else {
                sendEvent(stateContext.getStateMachine(), Events.RESULT_REJECTED);
            }
        };
    }

    private String getVariable(StateContext<States, Events> stateContext, String key) {
        return stateContext.getExtendedState().getVariables().get(key).toString();
    }

    private void sendEvent(StateMachine<States, Events> stateMachine, Events event) {
        var message = MessageBuilder.withPayload(event).build();
        stateMachine.sendEvent(Mono.just(message)).subscribe();
    }
}
