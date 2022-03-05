package com.camunda.training;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Check me on twitter here: https://twitter.com/cmnda_demo
@Component("createTweetDelegate")
public class CreateTweetDelegate implements JavaDelegate {
    private final Logger LOGGER = LoggerFactory.getLogger(CreateTweetDelegate.class.getName());

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        String content = (String) delegateExecution.getVariable("content");

        if (content.equals("Network error")) {
            throw new RuntimeException("simulated network error");
        }

        content = "Tj: " + content + "-" + LocalDateTime.now();
        LOGGER.info("Publishing tweet: " + content);

        // Call Service
        TwitterService twitterService = new TwitterService();
        twitterService.tweet(content);

        // Set process variables with tweetId
    }
}
