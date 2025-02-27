package com.camunda.training;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.springframework.stereotype.Service;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

@Service
public class TwitterService {

    public void tweet(String content) throws TwitterException {
        AccessToken accessToken = new AccessToken("220324559-CO8TfUmrcoCrvFHP4TacgToN5hLC8cMw4n2EwmHo", "WvVureFv5TBWTGhESgGe3fqZM7XbGMuyIhxB84zgcoUER");

        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer("lRhS80iIXXQtm6LM03awjvrvk", "gabtxwW8lnSL9yQUNdzAfgBOgIMSRqh7MegQs79GlKVWF36qLS");
            twitter.setOAuthAccessToken(accessToken);
            twitter.updateStatus(content);
        } catch (Exception te) {
            throw new BpmnError("dup_tweet_error", "This is a duplicate tweet");
        }
    }

}
