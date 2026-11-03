# Outbox Claim Token Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan.

**Goal:** 현재 선점자만 아웃박스 상태를 변경한다.
**Architecture:** 기존 조건부 UPDATE와 REQUIRES_NEW 트랜잭션에 UUID 토큰 비교를 추가한다. 별도 처리 계층이나 재시도 정책은 추가하지 않는다.
**Tech Stack:** Java 21, Spring Data JPA, MySQL, Flyway.
**Spec:** docs/superpowers/specs/2026-11-02-outbox-claim-token-design.md

## Global Constraints

- PDF와 커밋 직후 실행은 후속 브랜치에서 구현한다.
- 기존 사용자 파일은 보존한다. 테스트는 핵심 경합과 기존 회귀 확인에 한정한다.
- 만료 후 재선점 전에는 같은 토큰의 결과 기록을 허용한다.

## Review Focus

재선점 이전 토큰의 실패 기록, NULL 토큰, 최신 토큰 전달, 전이 후 토큰 정리, 구버전 작업자와 배포 중 공존 여부를 확인한다.

## Task 1: 선점 토큰 적용

**Files:** global/outbox의 Outbox, OutboxRepository, OutboxStateService, OutboxService 및 기존 OutboxStateServiceTest, OutboxServiceFailureTest; V40__add_outbox_claim_token.sql.

**Interfaces:** processing(Long id, long leaseDurationSeconds)는 Optional<String>을 반환한다. complete/cancel/fail(Long id, String claimToken), retry(Long id, String claimToken, int maxFailCount)는 boolean을 반환한다.

- [x] 기존 테스트를 새 인터페이스로 변경하고, 재선점 후 이전 토큰의 네 가지 결과 기록 거절 및 최신 토큰 성공을 매개변수 테스트로 추가한다.
- [x] 대상 테스트를 실행해 구현 전 실패를 확인한다.
- [x] nullable VARCHAR(36) 토큰 컬럼과 마이그레이션을 추가하고, 선점/결과 UPDATE와 호출부에 토큰을 전달한다.
- [x] 권한을 잃은 실패 결과는 처리 실패 집계에 포함하지 않는다.
- [x] `./gradlew test --tests 'com.forgather.global.outbox.*'`로 검증한다.
- [ ] 변경 범위를 검토하고 토큰 작업만 커밋/PR 대상으로 준비한다.

## 진행 기록

- 설계와 브랜치 승인 완료. 현재 작업 브랜치에서 진행한다.
- 실행 방식: 사용자 요청대로 최소 범위를 이 세션에서 직접 구현한다.

- 구현 전 새 인터페이스 부재로 테스트 컴파일 실패를 확인했다.
- 기존 미추적 PointHandlerAcceptanceTest가 없는 point 패키지를 참조하므로 외부 init script에서 해당 파일만 검증 제외했다. 사용자 파일은 변경하지 않았다.
- 코드 리뷰: 운영 결함 없음. 실패 집계 거절과 만료 후 재선점 전 완료 정책 테스트 두 개만 추가했다.

- 최종 검증: 외부 init script로 미추적 실험 테스트만 제외한 아웃박스, SocialRevokeHandler, WithdrawService, WithdrawAcceptance 대상 테스트 통과. git diff --check 통과.
