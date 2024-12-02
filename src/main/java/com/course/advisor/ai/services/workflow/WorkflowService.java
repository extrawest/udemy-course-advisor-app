package com.course.advisor.ai.services.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WorkflowService {
    private final StateMachineFactory<States, Events> factory;

    WorkflowService(StateMachineFactory<States, Events> factory) {
        this.factory = factory;
    }

    public String generateResult(String cvData, String requirements) {
        var resultFuture = new CompletableFuture<String>();
        var stateMachine = factory.getStateMachine();
        addStateListener(stateMachine, resultFuture);

        try {
            stateMachine.startReactively()
                    .doOnError(resultFuture::completeExceptionally)
                    .subscribe();

            stateMachine.getExtendedState().getVariables().put(Variables.INPUT, cvData);
            if (Objects.nonNull(requirements)) {
                stateMachine.getExtendedState().getVariables().put(Variables.REQUIREMENTS, requirements);
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

