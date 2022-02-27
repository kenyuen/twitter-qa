package com.camunda.training;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {
  private String content = "Tojimoto: Junit Happy Test -"+ LocalDateTime.now();

  @Test
  @Deployment(resources = "TwitterQA.bpmn")
  public void testHappyPath() {
    // Start process with Java API and variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("content",content);
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
    // Make assertions on the process instance
    assertThat(processInstance)
            .isWaitingAt("Activity_Review");
    // Get task lists in management group.
    List<Task> taskList = taskService()
            .createTaskQuery()
            .taskCandidateGroup("management")
            .processInstanceId(processInstance.getId())
            .list();
    // should only have on item
    assertThat(taskList).isNotNull();
    assertThat(taskList).hasSize(1);
    // get task
    Task task = taskList.get(0);
    // Complete task
    Map<String, Object> approvedMap = new HashMap<>();
    approvedMap.put("approved", true);
    approvedMap.put("content",content);
    taskService().complete(task.getId(), approvedMap);
    // Ensure completion with appropriate path
    assertThat(processInstance)
            .hasPassedInOrder("Event_Started","Activity_Review","Gateway_Approval","Activity_Publish","Event_Handled")
            .isEnded();
  }

  @Test
  @Deployment(resources = "TwitterQA.bpmn")
  public void testRejectPath() {
    // Create a HashMap to put in variables for the process instance
    Map<String, Object> variables = new HashMap<>();
    variables.put("content",content);
    // Start process with Java API and variables
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
    // get task and reject
    assertThat(processInstance).task().hasCandidateGroup("management").hasDefinitionKey("Activity_Review");
    complete(task(), withVariables("approved",false));
    // Make assertions on the process instance
    assertThat(processInstance)
            .hasPassedInOrder("Event_Started","Activity_Review","Gateway_Approval","Activity_Reject","Event_Rejected")
            .isEnded();
  }
}
