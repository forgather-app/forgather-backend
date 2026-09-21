package com.forgather.global.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("정적 팩토리 메서드")
    class StaticFactoryMethods {

        @DisplayName("success(data)는 SUCCESS 코드와 null 메시지로 데이터를 감싼다")
        @Test
        void success_withData() {
            // given
            String data = "hello";

            // when
            ApiResponse<String> response = ApiResponse.success(data);

            // then
            assertAll(
                () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(response.message()).isNull(),
                () -> assertThat(response.data()).isEqualTo(data)
            );
        }

        @DisplayName("success()는 데이터 없이 SUCCESS 코드만 담는다")
        @Test
        void success_noData() {
            // when
            ApiResponse<Void> response = ApiResponse.success();

            // then
            assertAll(
                () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(response.message()).isNull(),
                () -> assertThat(response.data()).isNull()
            );
        }

        @DisplayName("success(message, data)는 메시지와 데이터를 함께 담는다")
        @Test
        void success_withMessageAndData() {
            // given
            String message = "정상 처리되었습니다";
            Integer data = 42;

            // when
            ApiResponse<Integer> response = ApiResponse.success(message, data);

            // then
            assertAll(
                () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(response.message()).isEqualTo(message),
                () -> assertThat(response.data()).isEqualTo(data)
            );
        }

        @DisplayName("error(code, message)는 전달받은 코드와 메시지를 담고 데이터는 null이다")
        @Test
        void error_withCodeAndMessage() {
            // given
            String message = "잘못된 요청입니다";

            // when
            ApiResponse<Void> response = ApiResponse.error(ResponseCode.BAD_REQUEST, message);

            // then
            assertAll(
                () -> assertThat(response.code()).isEqualTo(ResponseCode.BAD_REQUEST),
                () -> assertThat(response.message()).isEqualTo(message),
                () -> assertThat(response.data()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("JSON 직렬화")
    class Serialization {

        @DisplayName("envelope 직렬화 결과는 code, message, data 세 필드만 갖는다 (null 포함)")
        @Test
        void serialize_keepsAllThreeFields() {
            // given
            ApiResponse<String> response = ApiResponse.success("hello");

            // when
            JsonNode node = objectMapper.valueToTree(response);

            // then
            assertThat(node.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("code", "message", "data");
        }

        @DisplayName("code는 SUCCESS 문자열로 직렬화되고 비어있는 message/data는 null로 표현된다")
        @Test
        void serialize_codeAsString_withNullFields() {
            // given
            ApiResponse<Void> response = ApiResponse.success();

            // when
            JsonNode node = objectMapper.valueToTree(response);

            // then
            assertAll(
                () -> assertThat(node.get("code").asText()).isEqualTo("SUCCESS"),
                () -> assertThat(node.get("message").isNull()).isTrue(),
                () -> assertThat(node.get("data").isNull()).isTrue()
            );
        }

        @DisplayName("error 응답은 code/message/data 세 필드만 갖고 code는 enum 이름 문자열, data는 null로 직렬화된다")
        @Test
        void serialize_errorEnvelope() {
            // given
            ApiResponse<Void> response = ApiResponse.error(ResponseCode.VALIDATION_FAILED, "유효하지 않은 입력입니다");

            // when
            JsonNode node = objectMapper.valueToTree(response);

            // then
            assertAll(
                () -> assertThat(node.fieldNames()).toIterable()
                    .containsExactlyInAnyOrder("code", "message", "data"),
                () -> assertThat(node.get("message").asText()).isEqualTo("유효하지 않은 입력입니다"),
                () -> assertThat(node.get("code").asText()).isEqualTo("VALIDATION_FAILED"),
                () -> assertThat(node.get("data").isNull()).isTrue()
            );
        }
    }
}
