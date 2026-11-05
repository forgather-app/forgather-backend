package com.forgather.global.config;

import java.util.List;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.client.ClientType;
import com.forgather.global.client.ClientVersionInterceptor;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    @Value("${api.base-url}")
    private String serverUrl;

    @Bean
    public OperationCustomizer clientTypeHeaderCustomizer() {
        return (operation, handlerMethod) -> operation.addParametersItem(
            new HeaderParameter()
                .name(ClientVersionInterceptor.CLIENT_TYPE_HEADER)
                .description("클라이언트 타입")
                .required(true)
                .schema(new StringSchema()._default(ClientType.WEB.name()))
        );
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .components(new Components()
                .addSecuritySchemes("cookieAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME)
                        .description("로그인 시 발급되는 HttpOnly access_token 쿠키로 인증합니다. "
                            + "브라우저에서는 로그인 후 쿠키가 자동으로 전송됩니다.")
                )
            )
            .servers(List.of(
                new Server().url(serverUrl).description("API Server")
            ));
    }

    private Info apiInfo() {
        return new Info()
            .title("Forgather API")
            .description("당신을 위한 순간, 흩어지지 않게. Forgather")
            .version("2.0.0");
    }
}
