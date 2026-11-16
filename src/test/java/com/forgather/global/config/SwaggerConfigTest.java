package com.forgather.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.QueryParameter;

class SwaggerConfigTest {

    @DisplayName("같은 API 정의를 반복 처리해도 클라이언트 타입 헤더는 하나만 추가된다")
    @Test
    void clientTypeHeaderIsAddedOnlyOnce() {
        Operation operation = new Operation();
        OperationCustomizer customizer = new SwaggerConfig().clientTypeHeaderCustomizer();

        customizer.customize(operation, null);
        customizer.customize(operation, null);

        assertThat(operation.getParameters()).singleElement().satisfies(parameter -> {
            assertThat(parameter.getName()).isEqualTo("X-Client-Type");
            assertThat(parameter.getIn()).isEqualTo("header");
            assertThat(parameter.getRequired()).isTrue();
            assertThat(parameter.getSchema().getDefault()).isEqualTo("WEB");
        });
    }

    @DisplayName("이름이 같은 쿼리 파라미터가 있어도 클라이언트 타입 헤더를 추가한다")
    @Test
    void queryParameterDoesNotPreventHeaderAddition() {
        QueryParameter queryParameter = new QueryParameter();
        queryParameter.setName("X-Client-Type");
        Operation operation = new Operation().addParametersItem(queryParameter);

        new SwaggerConfig().clientTypeHeaderCustomizer().customize(operation, null);

        assertThat(operation.getParameters()).hasSize(2).contains(queryParameter);
        assertThat(operation.getParameters()).filteredOn(parameter -> "header".equals(parameter.getIn()))
            .singleElement().satisfies(parameter ->
                assertThat(parameter.getName()).isEqualTo("X-Client-Type"));
    }
}
