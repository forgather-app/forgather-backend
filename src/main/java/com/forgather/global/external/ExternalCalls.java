package com.forgather.global.external;

import java.util.function.Supplier;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 외부 호출의 1차 분류. 상태코드와 IO 실패를 {@link FailureType}으로 옮긴다.
 * <p>
 * HTTP 자체는 호출부의 RestClient가 친다. 이 클래스는 넘겨받은 호출을 실행하고
 * 터진 예외에 이름표를 붙일 뿐이라 provider가 누구인지도, URL이 무엇인지도 모른다.
 * 상태가 없으므로 스프링 빈이 아니다.
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
                FailureType.MALFORMED_RESPONSE,
                null,
                "%s 응답을 해석할 수 없습니다.".formatted(operation.id()),
                e);
        }
    }
}
