package com.lapots.breed.snitch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handles S3 event. Expects only s3:ObjectCreated:* events.
 */
public class SnitchRuleCreationHandler implements RequestHandler<S3Event, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnitchRuleCreationHandler.class);

    private static final String AWS_JUDGE_ADD_URL = System.getenv("aws.judge.url");

    @Override
    public String handleRequest(S3Event input, Context context) {
        AmazonS3 s3client = AmazonS3ClientBuilder.defaultClient();

        LOGGER.debug("S3 events: {}.", input.getRecords().size());
        input.getRecords().forEach(record -> {
            String s3key = record.getS3().getObject().getKey();
            String s3bucket = record.getS3().getBucket().getName();
            LOGGER.info("Object was created: key={}, bucket={}", s3key, s3bucket);

            S3Object object = s3client.getObject(new GetObjectRequest(s3bucket, s3key));
            try (InputStream objectData = object.getObjectContent()) {
                String uploadedRule = IOUtils.toString(objectData);
                callRest(uploadedRule);
            } catch (IOException e) {
                LOGGER.error("Failed to close object stream!", e);
            }
        });
        return null;
    }

    /**
     * Does REST call to Judge Rest API.
     *
     * @param json rule json
     */
    private void callRest(String json) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(AWS_JUDGE_ADD_URL);
            StringEntity entity = new StringEntity(json);

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
