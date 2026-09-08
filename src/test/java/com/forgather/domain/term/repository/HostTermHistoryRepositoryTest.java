package com.forgather.domain.term.repository;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.REJECT;
import static com.forgather.domain.term.model.HostTermHistoryAction.WITHDRAW;
import static com.forgather.fixture.TermFixture.createMarketingTerm;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.jpa.HostTermHistoryJpaRepository;
import com.forgather.domain.term.repository.jpa.TermJpaRepository;
import com.forgather.fixture.HostFixture;

@DataJpaTest
class HostTermHistoryRepositoryTest {

    @Autowired
    private HostTermHistoryRepository hostTermHistoryRepository;

    @Autowired
    private HostTermHistoryJpaRepository hostTermHistoryJpaRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private TermJpaRepository termJpaRepository;

    @DisplayName("호스트가 동의한 약관 타입만 조회한다")
    @Test
    void findAgreedTermTypesByHostId() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        Term privacyTerm = termJpaRepository.save(createPrivacyTerm("1.0.0", "privacy"));
        hostTermHistoryRepository.saveAll(Set.of(
            new HostTermHistory(host, serviceTerm, AGREE),
            new HostTermHistory(host, privacyTerm, WITHDRAW)
        ));

        // when
        Set<TermType> agreedTermTypes = hostTermHistoryRepository.findAgreedTermTypesByHostId(host.getId());

        // then
        assertThat(agreedTermTypes).containsExactly(TermType.SERVICE);
    }

    @DisplayName("호스트의 약관 타입별 마지막 이력만 조회한다")
    @Test
    void findLatestHistoriesPerTypeByHost() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Term oldServiceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "old service"));
        Term latestServiceTerm = termJpaRepository.save(createServiceTerm("2.0.0", "latest service"));
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        hostTermHistoryRepository.saveAll(List.of(
            new HostTermHistory(host, oldServiceTerm, AGREE),
            new HostTermHistory(host, latestServiceTerm, WITHDRAW),
            new HostTermHistory(host, marketingTerm, REJECT)
        ));

        // when
        List<HostTermHistory> histories = hostTermHistoryRepository.findLatestHistoriesPerTypeByHost(host);

        // then
        assertAll(
            () -> assertThat(histories).hasSize(2),
            () -> assertThat(histories).extracting(history -> history.getTerm().getType())
                .containsExactlyInAnyOrder(TermType.SERVICE, TermType.MARKETING),
            () -> assertThat(histories).extracting(HostTermHistory::getAction)
                .containsExactlyInAnyOrder(WITHDRAW, REJECT)
        );
    }

    @DisplayName("최신 이력이 소프트 삭제되면 그 이전 이력을 최신으로 조회한다")
    @Test
    void findLatestHistoriesPerTypeByHostExcludesSoftDeletedLatestHistory() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        HostTermHistory oldHistory = new HostTermHistory(host, serviceTerm, AGREE);
        HostTermHistory latestHistory = new HostTermHistory(host, serviceTerm, WITHDRAW);
        hostTermHistoryRepository.saveAll(List.of(oldHistory, latestHistory));

        latestHistory.delete();
        hostTermHistoryJpaRepository.saveAndFlush(latestHistory);

        // when
        List<HostTermHistory> histories = hostTermHistoryRepository.findLatestHistoriesPerTypeByHost(host);

        // then
        assertAll(
            () -> assertThat(histories).hasSize(1),
            () -> assertThat(histories.get(0).getTerm().getType()).isEqualTo(TermType.SERVICE),
            () -> assertThat(histories.get(0).getAction()).isEqualTo(AGREE)
        );
    }

    @DisplayName("다른 호스트의 약관 이력은 조회하지 않는다")
    @Test
    void findLatestHistoriesPerTypeByHostExcludesOtherHosts() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Host otherHost = hostRepository.save(HostFixture.createHost());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        hostTermHistoryRepository.saveAll(List.of(
            new HostTermHistory(host, serviceTerm, AGREE),
            new HostTermHistory(otherHost, serviceTerm, WITHDRAW)
        ));

        // when
        List<HostTermHistory> histories = hostTermHistoryRepository.findLatestHistoriesPerTypeByHost(host);

        // then
        assertAll(
            () -> assertThat(histories).hasSize(1),
            () -> assertThat(histories.get(0).getAction()).isEqualTo(AGREE)
        );
    }
}
