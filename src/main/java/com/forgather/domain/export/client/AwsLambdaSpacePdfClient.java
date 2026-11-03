package com.forgather.domain.export.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.export.config.SpacePdfExportProperties;
import com.forgather.domain.export.dto.SpacePdfExportPayload;
import com.forgather.global.exception.BaseException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

@Component
public class AwsLambdaSpacePdfClient implements AsyncSpacePdfClient {

    private final LambdaClient lambdaClient;
    private final SpacePdfExportProperties properties;
    private final ObjectMapper objectMapper;

    public AwsLambdaSpacePdfClient(LambdaClient lambdaClient, SpacePdfExportProperties properties,
        ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void generate(String jobId) {
        properties.validateConfigured();
        try {
            Integer statusCode = lambdaClient.invoke(InvokeRequest.builder()
                .functionName(properties.functionName())
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(new SpacePdfExportPayload(jobId))))
                .build()).statusCode();
            if (!Integer.valueOf(HttpStatus.ACCEPTED.value()).equals(statusCode)) {
                throw new BaseException("PDF 람다 접수에 실패했습니다. jobId: %s, status: %s".formatted(jobId, statusCode),
                    HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch (JsonProcessingException e) {
            throw new BaseException("PDF 추출 payload 직렬화에 실패했습니다. jobId: %s".formatted(jobId),
                HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
