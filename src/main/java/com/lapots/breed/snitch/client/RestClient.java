package com.lapots.breed.snitch.client;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Rest client.
 */
public class RestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);

    /**
     * Does post request.
     * @param url url
     * @param payload payload
     */
    public void doPost(String url, String payload) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            StringEntity entity = new StringEntity(payload);

            request.setEntity(entity);
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-Type", "application/json");

            CloseableHttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                LOGGER.warn("Failed to do REST request. Returned code: [{}].", status);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to do REST call!", e);
        }
    }

}
