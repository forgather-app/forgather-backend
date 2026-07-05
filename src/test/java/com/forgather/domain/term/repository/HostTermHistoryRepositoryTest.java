package com.forgather.domain.term.repository;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.WITHDRAW;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.jpa.TermJpaRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;

@DataJpaTest
class HostTermHistoryRepositoryTest {

    @Autowired
    private HostTermHistoryRepository hostTermHistoryRepository;

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
}
