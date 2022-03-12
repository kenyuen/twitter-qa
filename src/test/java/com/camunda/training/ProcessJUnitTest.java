package com.camunda.training;

import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import twitter4j.TwitterException;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {
    private String content = "Happy -" + LocalDateTime.now();
//  private String content = "Network Error";

    @Mock
    private TwitterService mockTwitterService;

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testHappyPath() throws Exception {
        // Simple mock to bind the delegate
        MockitoAnnotations.initMocks(this);
        Mocks.register("createTweetDelegate", new CreateTweetDelegate(mockTwitterService));

        // Start process with Java API and variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("content", content);
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
        approvedMap.put("content", content);
        taskService().complete(task.getId(), approvedMap);
//    // Test asynch save point
        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();
        assertThat(jobList).hasSize(1);
        Job job = jobList.get(0);
        execute(job);

        // Ensure completion with appropriate path
        assertThat(processInstance)
                .hasPassedInOrder("Event_Started", "Activity_Review", "Gateway_Approval", "Activity_Publish", "Event_Handled")
                .isEnded();

//    Mockito.verify(mockTwitterService).tweet(content);
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testRejectPath() {
        // Create a HashMap to put in variables for the process instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("content", content);
        // Start process with Java API and variables
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
        // get task and reject
        assertThat(processInstance).task().hasCandidateGroup("management").hasDefinitionKey("Activity_Review");
        complete(task(), withVariables("approved", false));
        // Add external task handling
        assertThat(processInstance)
                .isWaitingAt(findId("Notify rejection"))
                .externalTask()
                .hasTopicName("notification");
        complete(externalTask());
        // Make assertions on the process instance
        assertThat(processInstance)
                .hasPassedInOrder("Event_Started", "Activity_Review", "Gateway_Approval", "Activity_Reject", "Event_Rejected")
                .isEnded();
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testTweetRejected() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("content", content);
        varMap.put("approved", false);
        // create process instance
        ProcessInstance processInstance = runtimeService()
                .createProcessInstanceByKey("TwitterQAProcess")
                .setVariables(varMap)
                .startAfterActivity((findId("Review Tweet")))
                .execute();
        // Add external task handling
        assertThat(processInstance)
                .isWaitingAt(findId("Notify rejection"))
                .externalTask()
                .hasTopicName("notification");
        complete(externalTask());
        // assert that it got rejected
        assertThat(processInstance)
                .isEnded()
                // note the starting point where activity has started is NOT from the beginning
                .hasPassedInOrder("Gateway_Approval", "Activity_Reject", "Event_Rejected")
                .hasPassed((findId("Notify rejection")));

    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testSuperUserTweet() throws Exception {
        // Simple mock to bind the delegate
        Mocks.register("createTweetDelegate", new CreateTweetDelegate(mockTwitterService));

        // create process instance
        ProcessInstance processInstance = runtimeService()
                .createMessageCorrelation("superuserTweet")
                .setVariable("content", "TJ Super User -" + System.currentTimeMillis())
                .correlateWithResult()
                .getProcessInstance();

        assertThat(processInstance).isStarted();

        // get the job
        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();

        // execute the job
        assertThat(jobList).hasSize(1);
        Job job = jobList.get(0);
        execute(job);

        assertThat(processInstance).isEnded();
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void tweetWithdrawn() throws Exception {
        // Simple mock to bind the delegate
        Mocks.register("createTweetDelegate", new CreateTweetDelegate(mockTwitterService));

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("content", "Test tweetWithdrawn message");
        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("TwitterQAProcess", varMap);
        assertThat(processInstance).isStarted().isWaitingAt(findId("Review Tweet"));
        runtimeService()
                .createMessageCorrelation("tweetWithdrawn")
                .processInstanceVariableEquals("content", "Test tweetWithdrawn message")
                .correlateWithResult();
        assertThat(processInstance)
                .isEnded()
                .hasPassedInOrder("Event_Started", "Activity_Review", "Event_Withdraw","Event_Withdrawn");
    }

}
