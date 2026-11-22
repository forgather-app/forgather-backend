package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.export.client.AsyncSpacePdfClient;
import com.forgather.domain.export.listener.SpacePdfExportListener;
import com.forgather.domain.export.model.SpacePdfExport;
import com.forgather.domain.export.repository.jpa.SpacePdfExportJpaRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.SpaceHostFixture;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.outbox.OutboxRepository;
import com.forgather.global.outbox.OutboxScheduler;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;

@AutoConfigureMockMvc
@TestPropertySource(properties = {"space.pdf-export.function-name=test-pdf", "space.pdf-export.region=ap-northeast-2"})
class SpacePdfExportAcceptanceTest extends AcceptanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private HostRepository hostRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private SpaceHostRepository spaceHostRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private SpacePdfExportJpaRepository exportRepository;
    @Autowired private OutboxRepository outboxRepository;

    @MockitoBean private AsyncSpacePdfClient pdfClient;
    @MockitoBean private SpacePdfExportListener exportListener;
    @MockitoBean private OutboxScheduler outboxScheduler;

    private MockedStatic<LocalDate> currentDate;
    private MockedStatic<LocalDateTime> currentDateTime;
    private Host host;
    private String token;
    private Space space;

    @BeforeEach
    void prepareExport() {
        currentDate = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
        currentDateTime = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
        setTime("2026-11-19T23:59:59");
        host = hostRepository.save(HostFixture.createHost());
        token = jwtTokenProvider.generateAccessToken(host.getId());
        space = createSpace(host, "pdfspace01");
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @AfterEach
    void closeCurrentDate() {
        if (currentDateTime != null) {
            currentDateTime.close();
        }
        if (currentDate != null) {
            currentDate.close();
        }
    }

    @Test
    @DisplayName("호출 이력이 없으면 오늘 사용 횟수 0과 한도 5를 반환한다")
    void returnsEmptyUsage() {
        // given & when & then
        usage(token).statusCode(200).body("data.count", equalTo(0)).body("data.limit", equalTo(5));
    }

    @Test
    @DisplayName("다섯 번째 추출은 허용하고 여섯 번째부터 저장 없이 400으로 거절한다")
    void rejectsRequestsAfterDailyLimit() {
        // given
        for (int i = 0; i < 5; i++) {
            request(token, space).statusCode(202);
        }
        long exports = exportRepository.count();
        long outboxes = outboxRepository.count();

        // when
        request(token, space).statusCode(400)
            .body("message", equalTo("오늘 PDF 추출 요청 횟수를 모두 사용했습니다. 하루 최대 5회 요청할 수 있습니다."));

        // then
        assertThat(exportRepository.count()).isEqualTo(exports);
        assertThat(outboxRepository.count()).isEqualTo(outboxes);
        usage(token).statusCode(200).body("data.count", equalTo(5)).body("data.limit", equalTo(5));
    }

    @Test
    @DisplayName("여러 스페이스의 요청을 합산하고 다른 사용자의 요청은 분리한다")
    void sharesLimitAcrossSpacesButNotHosts() {
        // given
        Space anotherSpace = createSpace(host, "pdfspace02");
        Host anotherHost = hostRepository.save(HostFixture.createHost());
        Space otherHostSpace = createSpace(anotherHost, "pdfspace03");
        String otherToken = jwtTokenProvider.generateAccessToken(anotherHost.getId());
        for (int i = 0; i < 4; i++) {
            request(token, space).statusCode(202);
        }
        request(token, anotherSpace).statusCode(202);

        // when & then
        request(token, space).statusCode(400);
        request(otherToken, otherHostSpace).statusCode(202);
        usage(token).statusCode(200).body("data.count", equalTo(5));
        usage(otherToken).statusCode(200).body("data.count", equalTo(1));
    }

    @Test
    @DisplayName("한국 시간 자정부터 전날 요청을 제외하고 새로 다섯 번 요청할 수 있다")
    void resetsAtKoreanMidnight() {
        // given
        for (int i = 0; i < 5; i++) {
            request(token, space).statusCode(202);
        }
        usage(token).statusCode(200).body("data.count", equalTo(5));

        // when
        setTime("2026-11-20T00:00:00");

        // then
        usage(token).statusCode(200).body("data.count", equalTo(0));
        for (int i = 0; i < 5; i++) {
            request(token, space).statusCode(202);
        }
        usage(token).statusCode(200).body("data.count", equalTo(5));
        request(token, space).statusCode(400);
        assertThat(exportRepository.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("한도를 초과한 이력은 실제 횟수를 반환하고 추가 요청을 거절한다")
    void returnsActualCountAboveLimit() {
        // given
        for (int i = 0; i < 6; i++) {
            SpacePdfExport export = new SpacePdfExport("concurrent-job-" + i, "{}", "reader@example.com", host.getId());
            exportRepository.saveAndFlush(export);
        }

        // when & then
        usage(token).statusCode(200).body("data.count", equalTo(6)).body("data.limit", equalTo(5));
        request(token, space).statusCode(400);
        assertThat(exportRepository.count()).isEqualTo(6);
    }

    @Test
    @DisplayName("미인증 사용자는 호출 횟수를 조회할 수 없다")
    void rejectsAnonymousUsage() {
        // given & when & then
        RestAssuredMockMvc.given().when().get("/hosts/me/exports/pdf/usage").then().statusCode(401);
    }

    private void setTime(String time) {
        LocalDateTime now = LocalDateTime.parse(time);
        LocalDate today = now.toLocalDate();
        currentDateTime.when(LocalDateTime::now).thenReturn(now);
        currentDate.when(() -> LocalDate.now(ZoneId.of("Asia/Seoul"))).thenReturn(today);
    }

    private Space createSpace(Host owner, String code) {
        Space created = spaceRepository.save(SpaceFixture.createSpaceWithCode(code));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(created, owner));
        return created;
    }

    private ValidatableMockMvcResponse request(String accessToken, Space target) {
        return RestAssuredMockMvc.given().postProcessors(withAccessToken(accessToken))
            .contentType(ContentType.JSON).body("{\"email\":\"reader@example.com\"}")
            .when().post("/spaces/{code}/exports/pdf", target.getCode()).then();
    }

    private ValidatableMockMvcResponse usage(String accessToken) {
        return RestAssuredMockMvc.given().postProcessors(withAccessToken(accessToken))
            .when().get("/hosts/me/exports/pdf/usage").then();
    }
}
