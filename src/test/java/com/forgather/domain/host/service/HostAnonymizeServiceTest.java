package com.forgather.domain.host.service;

import static com.forgather.fixture.HostFixture.createHostWithName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;

@Import(HostAnonymizeService.class)
@DataJpaTest
class HostAnonymizeServiceTest {

    @Autowired
    private HostAnonymizeService hostAnonymizeService;

    @Autowired
    private HostRepository hostRepository;

    private Host saveHostDeletedDaysAgo(String name, int days) {
        Host host = createHostWithName(name);
        ReflectionTestUtils.setField(host, "deletedAt", LocalDateTime.now().minusDays(days));
        return hostRepository.save(host);
    }

    @DisplayName("탈퇴 후 30일이 지난 회원만 익명화한다")
    @Test
    void anonymizeOnlyExpiredHosts() {
        // given
        Host expiredHost = saveHostDeletedDaysAgo("만료회원", 31);
        Host retainedHost = saveHostDeletedDaysAgo("보존회원", 29);
        Host activeHost = hostRepository.save(createHostWithName("활성회원"));

        // when
        hostAnonymizeService.anonymizeExpiredHosts();

        // then
        assertAll(
            () -> assertThat(expiredHost.getName()).isEqualTo("탈퇴한 회원"),
            () -> assertThat(expiredHost.getAnonymizedAt()).isNotNull(),
            () -> assertThat(retainedHost.getName()).isEqualTo("보존회원"),
            () -> assertThat(retainedHost.getAnonymizedAt()).isNull(),
            () -> assertThat(activeHost.getName()).isEqualTo("활성회원"),
            () -> assertThat(activeHost.getAnonymizedAt()).isNull()
        );
    }

    @DisplayName("이미 익명화된 회원은 다시 처리하지 않는다")
    @Test
    void skipAlreadyAnonymizedHosts() {
        // given
        Host host = createHostWithName("만료회원");
        ReflectionTestUtils.setField(host, "deletedAt", LocalDateTime.now().minusDays(31));
        LocalDateTime firstAnonymizedAt = LocalDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(host, "anonymizedAt", firstAnonymizedAt);
        hostRepository.save(host);

        // when
        hostAnonymizeService.anonymizeExpiredHosts();

        // then
        assertAll(
            () -> assertThat(host.getName()).isEqualTo("만료회원"),
            () -> assertThat(host.getAnonymizedAt()).isEqualTo(firstAnonymizedAt)
        );
    }
}
