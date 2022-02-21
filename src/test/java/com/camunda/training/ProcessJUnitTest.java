package com.camunda.training;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {

  @Test
  @Deployment(resources = "TwitterQA.bpmn")
  public void testHappyPath() {
    // Create a HashMap to put in variables for the process instance
    Map<String, Object> variables = new HashMap<>();
    variables.put("approved", true);
    variables.put("content","Exercise 4 test - tojimoto: "+ LocalDateTime.now());
    // Start process with Java API and variables
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
    // Make assertions on the process instance
    assertThat(processInstance)
            .hasPassedInOrder("Event_Started","Activity_Review","Gateway_Approval","Activity_Publish","Event_Handled")
            .isEnded();
  }

  @Test
  @Deployment(resources = "TwitterQA.bpmn")
  public void testRejectPath() {
    // Create a HashMap to put in variables for the process instance
    Map<String, Object> variables = new HashMap<>();
    variables.put("approved", false);
    // Start process with Java API and variables
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
    // Make assertions on the process instance
    assertThat(processInstance)
            .hasPassedInOrder("Event_Started","Activity_Review","Gateway_Approval","Activity_Reject","Event_Rejected")
            .isEnded();
  }
}
