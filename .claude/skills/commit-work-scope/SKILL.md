---
name: commit-work-scope
description: 구현이나 수정이 끝난 뒤 현재 작업트리의 변경사항을 검토하고, 사용자 변경사항을 보존한 채 요청된 변경 범위만 작업 단위별 한국어 커밋으로 나눈다. 사용자가 "커밋해줘", "작업 단위로 커밋", "변경사항 커밋", "구현 변경사항만 커밋", "$commit-work-scope"를 요청하면 사용한다.
---

# 작업 범위 커밋

구현 완료 후 변경사항을 섞지 않고 작업 단위별로 커밋한다. 핵심은 **범위 확인**, **사용자 변경 보존**, **명시적 path 커밋**이다.

## 기본 원칙

- 커밋 전에 `git status --short --branch`, `git diff --name-status`, `git diff --cached --name-status`로 작업트리와 staged 상태를 확인한다.
- 사용자가 요청한 구현 범위와 무관한 변경사항은 커밋하지 않는다.
- 이미 staged 된 무관한 파일이 있어도 되돌리거나 unstage 하지 않는다. 대신 `git commit --only -- <paths>`로 커밋 대상 파일을 명시한다.
- 사용자가 만든 것으로 보이는 변경사항을 삭제, 되돌리기, 덮어쓰기 하지 않는다.
- 커밋은 작업 단위가 자연스럽게 분리될 때만 나눈다. 예: 구현 변경과 테스트 추가는 별도 커밋으로 분리한다.
- 민감 설정 파일(`application*.yml`, secret, key, credential 등)을 새로 만들거나 수정했다면 커밋 전 `git-crypt status -e` 또는 필요한 `git-crypt status <path>`로 암호화 상태를 확인한다.

## 작업 절차

### 1. 변경 범위 확인

다음 명령으로 현재 상태를 읽는다.

```bash
git status --short --branch
git diff --name-status
git diff --cached --name-status
```

확인할 것:

- 이미 staged 된 파일이 있는지 확인한다.
- untracked 파일 중 이번 작업 산출물과 사용자 작업을 구분한다.
- 삭제/rename 파일이 의도한 작업 범위에 포함되는지 확인한다.
- 민감 파일이 포함되면 `.gitattributes`와 `git-crypt` 상태 확인이 필요한지 판단한다.

### 2. 작업 단위 나누기

변경사항을 커밋 가능한 의미 단위로 나눈다.

권장 분리:

- 구현 리팩터링 또는 기능 변경
- 테스트 추가 또는 테스트 보강
- 문서 변경
- 설정/빌드/CI 변경

한 커밋 안에는 같은 이유로 함께 변경되어야 하는 파일만 넣는다. 예를 들어 구현 파일과 그 구현의 필수 설정은 같은 커밋에 둘 수 있고, 테스트 파일은 별도 커밋으로 둘 수 있다.

### 3. 파일 스테이징

각 커밋 단위별로 필요한 파일만 명시해서 stage 한다.

```bash
git add path/to/file1 path/to/file2
```

staged 상태에 무관한 파일이 섞여 있으면 그대로 두고, 커밋 시 `--only`와 pathspec을 사용한다. 무관한 staged 파일을 임의로 `git restore --staged` 또는 `git reset` 하지 않는다.

### 4. 커밋 생성

커밋 제목은 다음 규칙을 따른다.

```text
<커밋 타입>: <작업 내용>
```

커밋 타입은 다음 중 하나만 사용한다.

```text
feature | refactor | chore | fix | docs | style | agent | cicd
```

작업 내용은 한국어를 기반으로 작성한다.
부가 설명이 필요하면 본문을 한국어 기반의 bullet로 작성한다.

```bash
git commit --only \
  -m "refactor: 소셜 JWKS 공개키 조회 공통화" \
  -m "- provider별 JWKS URL과 공개키 캐시를 공통 클라이언트로 분리" \
  -m "- 기존 Kakao 검증 흐름이 공통 클라이언트를 사용하도록 변경" \
  -- path/to/file1 path/to/file2
```

테스트만 분리하는 예:

```bash
git commit --only \
  -m "chore: 소셜 JWKS 공개키 조회 테스트 추가" \
  -m "- provider별 캐시 분리와 실패 정책 검증" \
  -- src/test/java/example/FooTest.java
```

### 5. 커밋 후 확인

각 커밋 후 다음을 확인한다.

```bash
git show --name-status --oneline --no-renames HEAD
git status --short --branch
```

마지막에는 최근 커밋 목록을 확인한다.

```bash
git log --oneline -n 3
```

최종 보고에는 다음을 포함한다.

- 생성된 커밋 해시와 제목
- 각 커밋의 작업 단위 요약
- 실행한 검증 명령과 결과
- 커밋하지 않고 남겨둔 사용자 변경사항
