package com.camunda.training;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("rejectionNotificationDelegate")
public class SendRejectionNotificationDelegate implements JavaDelegate {
    private final Logger LOGGER = LoggerFactory.getLogger(CreateTweetDelegate.class.getName());

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        String content = (String) delegateExecution.getVariable("content");
        LOGGER.info("Rejecting   : " + content);
    }
}
