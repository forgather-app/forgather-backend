package com.forgather.global.external;

import java.util.function.Supplier;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.forgather.global.exception.ExternalApiException;
import com.forgather.global.exception.ExternalFailureType;

/**
 * 외부 호출의 1차 분류. 상태코드와 IO 실패를 {@link ExternalFailureType}으로 옮긴다.
 */
public final class ExternalCalls {

    private ExternalCalls() {
    }

    public static <T> T execute(ExternalOperation operation, Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw ExternalApiException.fromStatus(operation, e);
        } catch (ResourceAccessException e) {
            throw ExternalApiException.fromIo(operation, e);
        } catch (RestClientException e) {
            // 역직렬화 실패 등. 외부가 계약을 바꿨거나 우리 DTO가 어긋난 경우라 error로 본다.
            throw new ExternalApiException(
                operation,
                ExternalFailureType.MALFORMED_RESPONSE,
                null,
                "%s 응답을 해석할 수 없습니다.".formatted(operation.id()),
                e);
        }
    }
}
