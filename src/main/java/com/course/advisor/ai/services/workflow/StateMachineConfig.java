package com.course.advisor.ai.services.workflow;

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

    private final RequirementsEvaluator requirementsEvaluator;
    private final ScriptGenerator scriptGenerator;
    private final SolutionVerifier solutionVerifier;
    private final RequirementsRewriter requirementsRewriter;

    StateMachineConfig(RequirementsEvaluator requirementsEvaluator,
            ScriptGenerator scriptGenerator,
            SolutionVerifier solutionVerifier,
            RequirementsRewriter requirementsRewriter) {
        this.requirementsEvaluator = requirementsEvaluator;
        this.scriptGenerator = scriptGenerator;
        this.solutionVerifier = solutionVerifier;
        this.requirementsRewriter = requirementsRewriter;
    }

    @Override
    public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
        states
                .withStates()
                .initial(States.AWAITING_INPUT)
                .state(States.CS_DATA_EXTRACTION, evaluateRequirementsAction())
                .state(States.RESULT_GENERATION, generateScriptAction())
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

    private Action<States, Events> evaluateRequirementsAction() {
        return stateContext -> {
            log.info("Evaluating requirements...");
            var requirements = getVariable(stateContext, Variables.CV_DATA);
            if (requirementsEvaluator.areRequirementsFeasible(requirements)) {
                sendEvent(stateContext.getStateMachine(), Events.REQUIREMENTS_EVALUATED);
            } else {
                sendEvent(stateContext.getStateMachine(), Events.REQUIREMENTS_REJECTED);
            }
        };
    }

    private Action<States, Events> generateScriptAction() {
        return stateContext -> {
            log.info("Generating script...");
            var requirements = getVariable(stateContext, Variables.CV_DATA);
            var script = scriptGenerator.generateScript(requirements);
            stateContext.getExtendedState().getVariables().put(Variables.RESULT, script);
            sendEvent(stateContext.getStateMachine(), Events.SCRIPT_GENERATED);
        };
    }

    private Action<States, Events> verifySolutionAction() {
        return stateContext -> {
            log.info("Verifying solution...");
            var requirements = getVariable(stateContext, Variables.CV_DATA);
            var script = getVariable(stateContext, Variables.RESULT);
            if (solutionVerifier.isScriptValid(requirements, script)) {
                sendEvent(stateContext.getStateMachine(), Events.SOLUTION_VERIFIED);
            } else {
                sendEvent(stateContext.getStateMachine(), Events.SOLUTION_REJECTED);
            }
        };
    }

    private Action<States, Events> rewriteRequirementsAction() {
        return stateContext -> {
            log.info("Rewriting requirements...");
            var requirements = getVariable(stateContext, Variables.CV_DATA);
            var script = getVariable(stateContext, Variables.RESULT);
            var rewrittenRequirements = requirementsRewriter.rewriteRequirements(requirements, script);
            stateContext.getExtendedState().getVariables().put(Variables.CV_DATA, rewrittenRequirements);
            sendEvent(stateContext.getStateMachine(), Events.REQUIREMENTS_REWRITTEN);
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
