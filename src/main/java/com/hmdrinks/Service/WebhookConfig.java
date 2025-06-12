package com.hmdrinks.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebhookConfig {

    private final String webhookUrlWeb;
    private final String webhookUrlAndroid;

    public WebhookConfig(@Value("${api.user-service.url}") String userServiceUrl) {
        this.webhookUrlWeb = userServiceUrl + "/callback/web/payOs";
        this.webhookUrlAndroid = userServiceUrl + "/callback/android/payOs";
    }

    public String getWebhookUrlWeb() {
        return webhookUrlWeb;
    }

    public String getWebhookUrlAndroid() {
        return webhookUrlAndroid;
    }
}
