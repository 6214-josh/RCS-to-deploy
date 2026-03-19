package com.rcs.system.wes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class WesCallbackClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${rcs.wes.base-url:http://127.0.0.1:8081/api/v1}")
    private String wesBaseUrl;

    @Value("${rcs.wes.token:}")
    private String wesToken;

    @Value("${rcs.wes.callback-enabled:true}")
    private boolean callbackEnabled;

    public void post(String path, Object body) {
        if (!callbackEnabled) {
            log.warn("WES callback disabled, skip {}{} body={}", wesBaseUrl, path, body);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (wesToken != null && !wesToken.isBlank()) {
            headers.setBearerAuth(wesToken);
        }

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(wesBaseUrl + path, HttpMethod.POST, entity, String.class);
        log.info("WES callback sent to {}{} status={} body={}", wesBaseUrl, path, response.getStatusCode(), response.getBody());
    }
}
