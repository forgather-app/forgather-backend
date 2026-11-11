package com.forgather.domain.export.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.export.config.SpacePdfExportProperties;
import com.forgather.global.exception.BaseException;

import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@ExtendWith(MockitoExtension.class)
class AwsLambdaSpacePdfClientTest {

    @Mock private LambdaClient sdkClient;

    @Test
    void sendsOnlyJobIdUsingEventInvocation() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AwsLambdaSpacePdfClient client = new AwsLambdaSpacePdfClient(sdkClient,
            new SpacePdfExportProperties("pdf-function", "ap-northeast-2"), mapper);
        when(sdkClient.invoke(any(InvokeRequest.class))).thenReturn(InvokeResponse.builder().statusCode(202).build());

        client.generate("f12be7f2-9022-480f-8510-f61b516706c1");

        ArgumentCaptor<InvokeRequest> request = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(sdkClient).invoke(request.capture());
        assertThat(request.getValue().functionName()).isEqualTo("pdf-function");
        assertThat(request.getValue().invocationType()).isEqualTo(InvocationType.EVENT);
        assertThat(mapper.readTree(request.getValue().payload().asUtf8String()))
            .isEqualTo(mapper.readTree("{\"jobId\":\"f12be7f2-9022-480f-8510-f61b516706c1\"}"));
    }
    @ParameterizedTest
    @ValueSource(ints = {200, 503})
    void rejectsResponsesOtherThanAccepted(int statusCode) {
        AsyncSpacePdfClient client = new AwsLambdaSpacePdfClient(sdkClient,
            new SpacePdfExportProperties("pdf-function", "ap-northeast-2"),
            new ObjectMapper());
        when(sdkClient.invoke(any(InvokeRequest.class)))
            .thenReturn(InvokeResponse.builder().statusCode(statusCode).build());

        assertThatThrownBy(() -> client.generate("f12be7f2-9022-480f-8510-f61b516706c1"))
            .isInstanceOfSatisfying(BaseException.class, error -> assertThat(error.getStatusCode()).isEqualTo(503));
    }

}
