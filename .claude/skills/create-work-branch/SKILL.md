---
name: create-work-branch
description: 작업 브랜치 분기 스킬. 사용자가 "작업 브랜치 분기", "작업 브랜치 생성", "브랜치 생성", "브랜치 만들어줘", "이슈 번호로 브랜치 생성", "$create-work-branch"를 요청하면 사용한다.
---

# 작업 브랜치 분기

작업 내용이 컨텍스트에 있거나 이슈 번호를 받았을 때 GitHub 이슈의 작업 내용을 확인하고, 최신 v2/develop에서 정해진 브랜치명 규칙으로 작업 브랜치를 분기한다.

## 작업 내용 확인

작업 내용이 컨텍스트에 있으면 그 내용을 사용한다.
이슈 번호를 받으면 GitHub에서 해당 이슈의 작업 내용을 확인한다.

## 브랜치 생성 규칙

### 분기 기점

분기는 최신 `v2/develop`에서 진행한다.

### 브랜치명

템플릿: `v2/<중간 prefix>/#<이슈번호>-<작업내용>`

- <중간 prefix>: `feature` | `refactor` | `chore` | `fix` | `docs` | `style` | `agent` | `cicd`
- <작업내용>: 이번 브랜치에서 작업할 내용을 영어로 간결하게 표현, 띄어쓰기 delimiter는 `-` 사용

예시:

- 회원 로그인 기능 구현 -> `v2/feature/#13-member-login`
- 방명록 신고 기능 리팩터링 -> `v2/refactor/#281-report-guestbook`
