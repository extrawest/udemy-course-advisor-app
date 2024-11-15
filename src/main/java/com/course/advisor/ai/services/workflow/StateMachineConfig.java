package com.course.advisor.ai.services.workflow;

import com.course.advisor.ai.services.agents.CVExtractionAgent;
import com.course.advisor.ai.services.agents.CurseRecommendationAgent;
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

@Configuration
@EnableStateMachineFactory
class StateMachineConfig extends StateMachineConfigurerAdapter<States, Events> {

    private static final Logger log = LoggerFactory.getLogger(StateMachineConfig.class);

    private final CurseRecommendationAgent curseRecommendationAgent;
    private final CVExtractionAgent cvExtractionAgent;

    StateMachineConfig(CurseRecommendationAgent curseRecommendationAgent, CVExtractionAgent cvExtractionAgent) {
        this.curseRecommendationAgent = curseRecommendationAgent;
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
                .withExternal().source(States.CS_DATA_EXTRACTION).target(States.RESULT_GENERATION).event(Events.REQUIREMENTS_EVALUATED).and()
                .withExternal().source(States.CS_DATA_EXTRACTION).target(States.FAILED_COMPLETION).event(Events.REQUIREMENTS_REJECTED).and()
                .withExternal().source(States.RESULT_GENERATION).target(States.SOLUTION_VERIFICATION).event(Events.SOLUTION_VERIFIED).and()
                .withExternal().source(States.SOLUTION_VERIFICATION).target(States.SUCCESSFUL_COMPLETION).event(Events.SOLUTION_VERIFIED).and()
                .withExternal().source(States.SOLUTION_VERIFICATION).target(States.RESULT_GENERATION).event(Events.SOLUTION_REJECTED);
    }

    private Action<States, Events> extractCvData() {
        return stateContext -> {
            log.info("Evaluating requirements...");
            var requirements = getVariable(stateContext, Variables.CV_DATA);
            if (cvExtractionAgent.answer(requirements)) {
                sendEvent(stateContext.getStateMachine(), Events.REQUIREMENTS_EVALUATED);
            } else {
                sendEvent(stateContext.getStateMachine(), Events.REQUIREMENTS_REJECTED);
            }
        };
    }

    private Action<States, Events> generateResult() {
        return stateContext -> {
            log.info("Generating script...");
            var requirements = getVariable(stateContext, Variables.CV_DATA);
            var script = curseRecommendationAgent.answer(requirements);
            stateContext.getExtendedState().getVariables().put(Variables.RESULT, script);
            sendEvent(stateContext.getStateMachine(), Events.SCRIPT_GENERATED);
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
