---
name: publish-pull-request
description: 현재 작업 브랜치의 커밋된 변경만 원격에 Push하고 v2/develop 대상 GitHub Pull Request를 생성·검증한다. 사용자가 "커밋 푸시하고 PR 만들어줘", "PR 올려줘", "작업 브랜치 Push 후 PR 생성", "커밋된 내용으로 PR 생성", "$publish-pull-request"처럼 Push와 PR 생성을 함께 요청할 때 사용한다.
---

# 커밋 Push 및 PR 생성

현재 브랜치에 이미 커밋된 변경만 게시한다. staged, unstaged, untracked 파일은 임의로 커밋하거나 PR에 포함하지 않는다.

## 1. 범위 확인

1. 저장소 지침과 PR 템플릿을 확인한다.
2. `git status --short --branch`로 브랜치와 미커밋 변경을 확인한다.
3. 현재 브랜치가 `v2/develop`이면 중단한다.
4. 다음 명령으로 PR 범위를 확인한다.

```bash
git log --oneline --no-merges v2/develop..HEAD
git diff --stat v2/develop...HEAD
git diff --name-status v2/develop...HEAD
```

- 포함할 커밋이 없으면 중단한다.
- 현재 브랜치의 기존 PR을 먼저 조회해 중복 생성을 막는다.
- 민감 설정 파일이 포함되고 저장소가 git-crypt를 사용하면 Push 전에 암호화 상태를 확인한다.

## 2. PR 내용 승인

커밋과 diff, PR 템플릿, 최근 PR 형식을 근거로 제목과 본문을 작성한다.

- 제목은 반드시 `<작업 타입>: <작업 내용 요약>` 형식으로 작성한다.
  - <작업 타입>: `feature`, `refactor`, `cicd`, `chore`, `fix`, `style`, `docs`
- 본문에는 연관 이슈, 핵심 변경, 설계 의도, 검증 결과만 담는다.
- 추측이 필요한 설계 의도는 사용자에게 확인한다.

Push 전에 source 브랜치, draft 여부, 제목, 본문, 제외할 미커밋 파일을 보여주고 승인받는다. 사용자가 같은 내용을 이미 승인하고 즉시 생성을 지시했다면 다시 묻지 않는다.

## 3. Push 및 PR 생성

1. `gh auth status`로 인증을 확인한다. 실패하면 중단하고 재인증을 안내한다.
2. 강제 Push 없이 현재 브랜치를 게시한다.

```bash
git push -u origin HEAD
```

3. `v2/develop`을 base 브랜치로 지정해 PR을 생성한다.
4. GitHub 연동 도구를 우선 사용하고, 권한이 없으면 인증된 `gh pr create`로 대체한다.
5. CLI 사용 시 본문을 임시 파일에 저장해 실제 줄바꿈을 보존한다.
6. 사용자가 지정하지 않은 경우 draft로 생성한다.

## 4. 결과 검증

PR을 다시 조회해 제목, 본문, `v2/develop` base, source 브랜치, draft 여부, URL을 확인한다.

최종 보고에는 PR 링크, 브랜치, 검증 결과, PR에 포함하지 않은 로컬 변경을 명시한다.
