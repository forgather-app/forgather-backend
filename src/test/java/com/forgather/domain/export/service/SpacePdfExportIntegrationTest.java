package com.forgather.domain.export.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.container.TestOnContainer;
import com.forgather.domain.export.client.AsyncSpacePdfClient;
import com.forgather.domain.export.config.SpacePdfExportProperties;
import com.forgather.domain.export.dto.ExportSpacePdfRequest;
import com.forgather.domain.export.dto.ExportSpacePdfResponse;
import com.forgather.domain.export.model.SpacePdfExport;
import com.forgather.domain.export.repository.jpa.SpacePdfExportJpaRepository;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookCardPhoto;
import com.forgather.domain.guestbook.repository.GuestBookCardPhotoRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpaceHost;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.InvalidConfigurationException;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxRepository;
import com.forgather.global.outbox.OutboxScheduler;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxStatus;
import com.forgather.global.outbox.OutboxType;

import jakarta.servlet.http.Cookie;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "space.pdf-export.function-name=pdf-test",
    "space.pdf-export.region=ap-northeast-2",
    "space.pdf-export.public-url-base=https://images.example.com/root/"
})
@Import(SpacePdfExportIntegrationTest.Fakes.class)
@AutoConfigureMockMvc
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SpacePdfExportIntegrationTest extends TestOnContainer {

    @Autowired private MockMvc mockMvc;
    @Autowired private HostRepository hostRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private SpaceHostRepository spaceHostRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private OutboxScheduler outboxScheduler;

    @Autowired private SpacePdfExportService service;
    @Autowired private SpacePdfExportJpaRepository exportRepository;
    @Autowired private GuestBookCardRepository cardRepository;
    @Autowired private GuestBookCardPhotoRepository photoRepository;
    @Autowired private OutboxService outboxService;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private FakeGenerationClient generationClient;
    @MockitoBean(name = "taskExecutor") private TaskExecutor taskExecutor;

    private Host owner;
    private Space ownedSpace;
    private final List<Runnable> submitted = new ArrayList<>();

    @BeforeEach
    void prepare() {
        owner = hostRepository.save(HostFixture.createHost());
        ownedSpace = spaceRepository.save(SpaceFixture.createSpaceWithCode("abcdefghij"));
        spaceHostRepository.save(new SpaceHost(ownedSpace, owner));
        submitted.clear();
        generationClient.jobIds.clear();
        generationClient.isFailing = false;
        doAnswer(invocation -> {
            submitted.add(invocation.getArgument(0));
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));
    }

    @Test
    void acceptsPdfExportAndReturnsJobId() throws Exception {
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spaceHostRepository.save(new SpaceHost(space, host));

        mockMvc.perform(post("/spaces/{spaceCode}/exports/pdf", space.getCode())
                .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                    jwtTokenProvider.generateAccessToken(host.getId())))
                .contentType("application/json")
                .content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.jobId").isNotEmpty());
    }
    @Test
    void snapshotsOnlyVisibleCardsAndLivePhotosInStableOrder() throws Exception {
        GuestBookCard newer = cardRepository.save(new GuestBookCard(ownedSpace, "newer", "new message"));
        GuestBookCard older = cardRepository.save(new GuestBookCard(ownedSpace, "older", "old message"));
        GuestBookCard tied = cardRepository.save(new GuestBookCard(ownedSpace, "tied", "same time"));
        GuestBookCard hidden = new GuestBookCard(ownedSpace, "hidden", "hidden message");
        hidden.hideByAdmin();
        cardRepository.save(hidden);
        GuestBookCard deleted = new GuestBookCard(ownedSpace, "deleted", "deleted message");
        deleted.delete();
        cardRepository.save(deleted);
        Space other = spaceRepository.save(SpaceFixture.createSpaceWithCode("otherother"));
        cardRepository.save(new GuestBookCard(other, "other", "other space"));
        jdbcTemplate.update("UPDATE guest_book_card SET created_at = '2020-01-02 03:04:05' WHERE id = ?", newer.getId());
        jdbcTemplate.update("UPDATE guest_book_card SET created_at = '2020-01-01 03:04:05' WHERE id IN (?, ?)",
            older.getId(), tied.getId());
        GuestBookCardPhoto removed = new GuestBookCardPhoto("gone", "gone.jpg", 1L, older);
        removed.delete();
        GuestBookCardPhoto firstPhoto = new GuestBookCardPhoto("first", "folder/한 글?#%.jpg", 1L, older);
        GuestBookCardPhoto secondPhoto = new GuestBookCardPhoto("second", "/second.jpg", 1L, older);
        GuestBookCardPhoto tiedPhoto = new GuestBookCardPhoto("tied", "tied.jpg", 1L, older);
        photoRepository.saveAll(List.of(firstPhoto, removed, secondPhoto, tiedPhoto,
            new GuestBookCardPhoto("hidden", "hidden.jpg", 1L, hidden)));
        jdbcTemplate.update("UPDATE guest_book_card_photo SET created_at = '2020-01-02 03:04:05' WHERE id = ?",
            firstPhoto.getId());
        jdbcTemplate.update("UPDATE guest_book_card_photo SET created_at = '2020-01-01 03:04:05' WHERE id IN (?, ?)",
            secondPhoto.getId(), tiedPhoto.getId());

        ExportSpacePdfResponse response = service.request(ownedSpace.getCode(), owner,
            new ExportSpacePdfRequest("reader@example.com"));
        SpacePdfExport stored = exportRepository.findByJobId(response.jobId()).orElseThrow();
        assertThat(stored.getId()).isPositive();
        JsonNode snapshot = objectMapper.readTree(stored.getSnapshot());
        assertThat(stored.getEmail()).isEqualTo("reader@example.com");
        assertThat(snapshot).isEqualTo(objectMapper.readTree("""
            {"spaceName":"name","nickname":"포스티","guestbookInfos":[
              {"author":"older","message":"old message","createdAt":"2020-01-01T03:04:05",
               "photoUrls":["https://images.example.com/root/second.jpg",
                            "https://images.example.com/root/tied.jpg",
                            "https://images.example.com/root/folder/%ED%95%9C%20%EA%B8%80%3F%23%25.jpg"]},
              {"author":"tied","message":"same time","createdAt":"2020-01-01T03:04:05","photoUrls":[]},
              {"author":"newer","message":"new message","createdAt":"2020-01-02T03:04:05","photoUrls":[]}
            ]}
            """));
        assertThat(cardRepository.findByIdAndDeletedAtIsNull(older.getId()).orElseThrow().isRead()).isFalse();
        assertThat(objectMapper.readTree(outboxRepository.findAll().getFirst().getPayload()))
            .isEqualTo(objectMapper.readTree("{\"jobId\":\"" + response.jobId() + "\"}"));
        assertThat(service.request(ownedSpace.getCode(), owner, new ExportSpacePdfRequest("reader@example.com")).jobId())
            .isNotEqualTo(response.jobId());
        assertThat(UUID.fromString(response.jobId())).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"email\":null}", "{\"email\":\"\"}",
        "{\"email\":\" \"}", "{\"email\":\"invalid\"}"})
    void rejectsInvalidEmail(String json) throws Exception {
        mockMvc.perform(request(owner).content(json)).andExpect(status().isBadRequest());
        assertThat(exportRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void rejectsOverlongEmail() throws Exception {
        mockMvc.perform(request(owner).content(objectMapper.writeValueAsString(
                new ExportSpacePdfRequest("a".repeat(309) + "@example.com"))))
            .andExpect(status().isBadRequest());
        assertThat(exportRepository.count()).isZero();
    }

    @Test
    void rejectsNonOwnerAndAnonymousRequests() throws Exception {
        Host other = hostRepository.save(HostFixture.createHost());
        mockMvc.perform(request(other).content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/spaces/{code}/exports/pdf", ownedSpace.getCode())
                .contentType("application/json").content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isUnauthorized());
        assertThat(exportRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void rejectsDuplicateJobIdWithSeparatePrimaryKey() {
        String jobId = UUID.randomUUID().toString();
        SpacePdfExport saved = exportRepository.saveAndFlush(
            new SpacePdfExport(jobId, "{}", "reader@example.com"));

        assertThat(saved.getId()).isPositive();
        assertThat(exportRepository.findById(saved.getId()).orElseThrow().getJobId()).isEqualTo(jobId);
        assertThatThrownBy(() -> exportRepository.saveAndFlush(
            new SpacePdfExport(jobId, "{}", "other@example.com")))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(exportRepository.findByJobId(jobId).orElseThrow().getEmail()).isEqualTo("reader@example.com");
    }

    @Test
    void dispatchesAfterCommitOnly() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String jobId = transaction.execute(status -> {
            String id = service.request(ownedSpace.getCode(), owner,
                new ExportSpacePdfRequest("reader@example.com")).jobId();
            assertThat(submitted).isEmpty();
            assertThat(generationClient.jobIds).isEmpty();
            return id;
        });
        assertThat(submitted).hasSize(1);
        assertThat(exportRepository.findByJobId(jobId)).isPresent();
        submitted.getFirst().run();
        assertThat(generationClient.jobIds).containsExactly(jobId);
        assertThat(outboxRepository.findAll().getFirst().getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    }

    @Test
    void rollsBackSnapshotAndOutboxWithoutDispatch() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.request(ownedSpace.getCode(), owner, new ExportSpacePdfRequest("reader@example.com"));
            status.setRollbackOnly();
        });
        assertThat(exportRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
        assertThat(submitted).isEmpty();
        assertThat(generationClient.jobIds).isEmpty();
    }

    @Test
    void schedulerRecoversRejectedSubmission() {
        doThrow(new RejectedExecutionException("full")).when(taskExecutor).execute(any(Runnable.class));
        String jobId = service.request(ownedSpace.getCode(), owner,
            new ExportSpacePdfRequest("reader@example.com")).jobId();
        assertThat(generationClient.jobIds).isEmpty();
        assertThat(outboxRepository.findAll().getFirst().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxService.process().succeededCount()).isEqualTo(1);
        assertThat(generationClient.jobIds).containsExactly(jobId);
    }

    @Test
    void retriesSameJobAndCompletesAfterAcceptance() {
        String jobId = service.request(ownedSpace.getCode(), owner,
            new ExportSpacePdfRequest("reader@example.com")).jobId();
        Long outboxId = outboxRepository.findAll().getFirst().getId();
        generationClient.isFailing = true;
        submitted.getFirst().run();
        Outbox pending = outboxRepository.findById(outboxId).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(pending.getFailCount()).isEqualTo(1);
        generationClient.isFailing = false;
        assertThat(outboxService.process().succeededCount()).isEqualTo(1);
        assertThat(generationClient.jobIds).containsExactly(jobId, jobId);
        assertThat(outboxRepository.findById(outboxId).orElseThrow().getPayload()).isNull();
        assertThat(outboxService.process(outboxId).isEmpty()).isTrue();
        assertThat(outboxService.process(Long.MAX_VALUE).isEmpty()).isTrue();
        assertThat(generationClient.jobIds).hasSize(2);
    }

    @Test
    void stopsAfterFiveFailuresAndRejectsMalformedPayload() {
        String jobId = service.request(ownedSpace.getCode(), owner,
            new ExportSpacePdfRequest("reader@example.com")).jobId();
        Long outboxId = outboxRepository.findAll().getFirst().getId();
        generationClient.isFailing = true;
        for (int i = 0; i < 5; i++) {
            assertThat(outboxService.process(outboxId).failedCount()).isEqualTo(1);
        }
        assertThat(outboxRepository.findById(outboxId).orElseThrow().getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(generationClient.jobIds).containsExactly(jobId, jobId, jobId, jobId, jobId);
        assertThat(outboxService.process(outboxId).isEmpty()).isTrue();
        Outbox invalid = outboxRepository.save(Outbox.pending(OutboxType.SPACE_PDF_EXPORT, "{\"jobId\":\" \"}"));
        assertThat(outboxService.process(invalid.getId()).failedCount()).isEqualTo(1);
        assertThat(outboxRepository.findById(invalid.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(generationClient.jobIds).hasSize(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-function", "missing-region", "missing-base", "invalid-base"})
    void rejectsUnconfiguredFeatureWithoutSavingJob(String missing) {
        SpacePdfExportProperties properties = new SpacePdfExportProperties(
            missing.equals("missing-function") ? null : "pdf-test",
            missing.equals("missing-region") ? " " : "ap-northeast-2",
            missing.equals("missing-base") ? null : missing.equals("invalid-base") ? "file:///tmp/photos"
                : "https://images.example.com");
        SpacePdfExportService unconfigured = new SpacePdfExportService(spaceRepository, spaceHostRepository,
            cardRepository, photoRepository, exportRepository, properties, objectMapper, outboxService, eventPublisher);

        assertThatThrownBy(() -> unconfigured.request(ownedSpace.getCode(), owner,
            new ExportSpacePdfRequest("reader@example.com")))
            .isExactlyInstanceOf(InvalidConfigurationException.class)
            .satisfies(error -> assertThat(((InvalidConfigurationException)error).getStatusCode()).isEqualTo(503));
        assertThat(exportRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
        assertThat(submitted).isEmpty();
    }

    private MockHttpServletRequestBuilder request(Host host) {
        return post("/spaces/{code}/exports/pdf", ownedSpace.getCode())
            .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                jwtTokenProvider.generateAccessToken(host.getId())))
            .contentType("application/json");
    }

    @TestConfiguration
    static class Fakes {

        @Bean
        @Primary
        FakeGenerationClient fakeGenerationClient() {
            return new FakeGenerationClient();
        }
    }

    static class FakeGenerationClient implements AsyncSpacePdfClient {

        private final List<String> jobIds = new ArrayList<>();
        private boolean isFailing;

        @Override
        public void generate(String jobId) {
            jobIds.add(jobId);
            if (isFailing) {
                throw new IllegalStateException("PDF 생성 요청 접수 실패");
            }
        }
    }
}
