package com.forgather.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import com.forgather.global.util.RandomCodeGenerator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class RequestBodyLoggingTest {

    private final Logger logger = (Logger)LoggerFactory.getLogger(LoggingInterceptor.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private ch.qos.logback.classic.Level previousLevel;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        previousLevel = logger.getLevel();
        logger.setLevel(ch.qos.logback.classic.Level.TRACE);
        appender.start();
        logger.addAppender(appender);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .addInterceptors(new LoggingInterceptor(new RandomCodeGenerator()))
            .addFilters(new RequestWrappingFilter())
            .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new MappingJackson2HttpMessageConverter())
            .build();
    }

    @AfterEach
    void restoreLogger() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

    @DisplayName("어노테이션이 없으면 요청 본문이 기록되지 않고 컨트롤러에 정상 전달된다.")
    @Test
    void unannotatedBodyIsNotLogged() throws Exception {
        mockMvc.perform(post("/unannotated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hello\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string("hello"));

        assertThat(getBodyLogs()).isEmpty();
    }

    @DisplayName("어노테이션이 있으면 지정한 레벨로 원본 본문을 기록하고 컨트롤러에 정상 전달한다.")
    @ParameterizedTest
    @CsvSource({"/default, DEBUG", "/trace, TRACE", "/info, INFO", "/warn, WARN", "/error, ERROR"})
    void annotatedBodyIsLoggedAtSelectedLevel(String path, String level) throws Exception {
        String body = "{ \"message\" : \"안녕하세요\" }";
        mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.getBytes(StandardCharsets.UTF_8)))
            .andExpect(status().isOk())
            .andExpect(content().string("안녕하세요"));

        assertThat(getBodyLogs()).singleElement().satisfies(event -> {
            assertThat(event.getLevel().toString()).isEqualTo(level);
            assertThat(event.getFormattedMessage()).isEqualTo("\n" + body);
            assertThat(event.getMarkerList()).extracting(marker -> marker.getName()).containsExactly("BODY");
        });
    }

    @DisplayName("선택한 로그 레벨이 비활성화되어 있으면 본문 로깅 없이 요청을 처리한다.")
    @Test
    void disabledLevelDoesNotLogBody() throws Exception {
        logger.setLevel(ch.qos.logback.classic.Level.INFO);

        mockMvc.perform(post("/default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hello\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string("hello"));

        assertThat(getBodyLogs()).isEmpty();
    }

    @DisplayName("본문에 지정된 문자 인코딩으로 로깅하고 요청을 정상 처리한다.")
    @Test
    void bodyUsesDeclaredCharset() throws Exception {
        String body = "{\"message\":\"café\"}";
        mockMvc.perform(post("/default")
                .contentType(new MediaType("application", "json", StandardCharsets.ISO_8859_1))
                .content(body.getBytes(StandardCharsets.ISO_8859_1)))
            .andExpect(status().isOk())
            .andExpect(content().string("café"));

        assertThat(getBodyLogs()).singleElement()
            .extracting(ILoggingEvent::getFormattedMessage).isEqualTo("\n" + body);
    }

    @DisplayName("charset이 없거나 잘못되어도 UTF-8로 본문을 기록하고 응답 헤더 설정과 MDC 정리를 완료한다.")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"invalid-charset", "UTF 8"})
    void invalidCharsetFallsBackOnlyForLogging(String encoding) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/default") {

            @Override
            public String getCharacterEncoding() {
                return encoding;
            }
        };
        String body = "{\"message\":\"안녕하세요\"}";
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        CustomRequestBodyWrapper wrapper = new CustomRequestBodyWrapper(request);
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = new HandlerMethod(new TestController(),
            TestController.class.getDeclaredMethod("defaultLevel", Map.class));
        LoggingInterceptor interceptor = new LoggingInterceptor(new RandomCodeGenerator());
        interceptor.preHandle(wrapper, response, handler);
        String traceId = MDC.get("traceId");

        interceptor.afterCompletion(wrapper, response, handler, null);

        assertThat(getBodyLogs()).singleElement()
            .extracting(ILoggingEvent::getFormattedMessage).isEqualTo("\n" + body);
        assertThat(request.getCharacterEncoding()).isEqualTo(encoding);
        assertThat(response.getHeader("trace-id")).isEqualTo(traceId);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @DisplayName("어노테이션이 있으면 JSON 변환에 실패한 요청도 원본 본문을 기록한다.")
    @Test
    void malformedAnnotatedBodyIsLogged() throws Exception {
        mockMvc.perform(post("/default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid-json"))
            .andExpect(status().isBadRequest());

        assertThat(getBodyLogs()).singleElement()
            .extracting(ILoggingEvent::getFormattedMessage).isEqualTo("\ninvalid-json");
    }

    @DisplayName("캐싱 필터가 없으면 어노테이션이 있어도 본문을 기록하지 않는다.")
    @Test
    void annotatedBodyWithoutCachingFilterIsNotLogged() throws Exception {
        MockMvc mockMvcWithoutFilter = MockMvcBuilders.standaloneSetup(new TestController())
            .addInterceptors(new LoggingInterceptor(new RandomCodeGenerator()))
            .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new MappingJackson2HttpMessageConverter())
            .build();

        mockMvcWithoutFilter.perform(post("/default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hello\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string("hello"));

        assertThat(getBodyLogs()).isEmpty();
    }

    private List<ILoggingEvent> getBodyLogs() {
        return appender.list.stream()
            .filter(event -> event.getMarkerList() != null
                && event.getMarkerList().stream().anyMatch(marker -> marker.getName().equals("BODY")))
            .toList();
    }

    @RestController
    static class TestController {

        @PostMapping("/unannotated")
        String unannotated(@RequestBody Map<String, String> body) {
            return body.get("message");
        }

        @LogRequestBody
        @PostMapping("/default")
        String defaultLevel(@RequestBody Map<String, String> body) {
            return body.get("message");
        }

        @LogRequestBody(level = Level.TRACE)
        @PostMapping("/trace")
        String trace(@RequestBody Map<String, String> body) {
            return body.get("message");
        }

        @LogRequestBody(level = Level.INFO)
        @PostMapping("/info")
        String info(@RequestBody Map<String, String> body) {
            return body.get("message");
        }

        @LogRequestBody(level = Level.WARN)
        @PostMapping("/warn")
        String warn(@RequestBody Map<String, String> body) {
            return body.get("message");
        }

        @LogRequestBody(level = Level.ERROR)
        @PostMapping("/error")
        String error(@RequestBody Map<String, String> body) {
            return body.get("message");
        }
    }
}
