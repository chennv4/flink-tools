package io.pravega.flinktools.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;

public class SlackNotifier {
    static Logger log = LoggerFactory.getLogger(SlackNotifier.class);
    private String slack_userName;
    private String slack_channelName;
    private String slack_webhook;
    private boolean slack_msg_enabled;

    public  SlackNotifier(String slack_userName,
                          String slack_channelName,
                          String slack_webhook,
                          boolean slack_msg_enabled
    ){
        this.slack_userName = slack_userName;
        this.slack_channelName = slack_channelName;
        this.slack_webhook = slack_webhook;
        this.slack_msg_enabled = slack_msg_enabled;
    }



    public void notify(String message) {
        try{
            if(slack_msg_enabled)
            {
                JSONObject json = new JSONObject();
                json.put("username", slack_userName);
                json.put("channel", slack_channelName);
                json.put("text", message);
                Request.Post(slack_webhook)
                        .bodyString(json.toString(), ContentType.APPLICATION_JSON)
                        .execute();
            }

        }catch (Exception e){
            log.warn("Failed to send notification {}: {}", message, e.toString());
            e.printStackTrace();
        }
    }

    public static void main(String... args) throws Exception {
        try {
            SlackNotifier  notifier = new SlackNotifier(
                    "Chenna",
                    "#nautilus-lab-alerts",
                    "https://hooks.slack.com/services/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",
                    true
            );
            notifier.notify("This is a *line* of test message.\n And this is second line. ");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}

