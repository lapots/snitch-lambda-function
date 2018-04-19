package com.lapots.breed.snitch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handles S3 event. Expects only s3:ObjectCreated:* events.
 */
public class SnitchRuleCreationHandler implements RequestHandler<S3Event, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnitchRuleCreationHandler.class);

    @Override
    public String handleRequest(S3Event input, Context context) {
        AmazonS3 s3client = AmazonS3ClientBuilder.defaultClient();

        LOGGER.debug("S3 events: {}.", input.getRecords().size());
        input.getRecords().forEach(record -> {
            String s3key = record.getS3().getObject().getKey();
            String s3bucket = record.getS3().getBucket().getName();
            LOGGER.info("Object was created: key={}, bucket={}", s3key, s3bucket);

            S3Object object = s3client.getObject(new GetObjectRequest(s3bucket, s3key));
            InputStream objectData = object.getObjectContent();
            try {
                String uploadedRule = IOUtils.toString(objectData);
                objectData.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close object stream!", e);
            }
        });
        return null;
    }
}
