# JPA 성능 해결 패턴

`jpa-analyzer` 에이전트가 N+1/성능 이슈를 발견했을 때 제안하는 4가지 해결 패턴의 상세 예시와 주의사항.

메인 에이전트: `.claude/agents/jpa-analyzer.md`

## 패턴 카탈로그

| # | 패턴 | 용도 | 문서 |
|---|---|---|---|
| 1 | **Fetch Join** | 연관 엔티티 즉시 로딩 (N+1 직접 해결) | [01-fetch-join.md](./01-fetch-join.md) |
| 2 | **EntityGraph** | 동적 페치 전략 지정 (선언적·재사용) | [02-entity-graph.md](./02-entity-graph.md) |
| 3 | **DTO 프로젝션** | 필요한 필드만 조회 (불필요한 데이터 제거) | [03-dto-projection.md](./03-dto-projection.md) |
| 4 | **배치 쿼리** | IN 절로 루프 내 개별 조회 해소 | [04-batch-query.md](./04-batch-query.md) |

## 패턴 선택 가이드

| 상황 | 우선 적용 |
|---|---|
| 단일 엔티티의 명확한 연관 로딩 | **Fetch Join** |
| 같은 엔티티에 상황별 다른 로딩 전략 필요 | **EntityGraph** |
| 엔티티의 일부 필드만 필요 / 집계 쿼리 | **DTO 프로젝션** |
| 부모 리스트에서 자식 데이터를 루프로 조회 | **배치 쿼리** (Map 기반 O(1)) |
| 페이징 + 컬렉션 로딩 동시 필요 | **DTO 프로젝션** 또는 `@BatchSize` |
