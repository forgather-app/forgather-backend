---
name: test-writer
description: 테스트 코드 생성. unit/service/acceptance 테스트를 프로젝트 컨벤션에 맞춰 작성. Use when user says "테스트 작성해줘", "test 코드 만들어줘", "단위 테스트 추가", "인수 테스트 작성", "서비스 테스트", or mentions JUnit, RestAssured, test fixture.
allowed-tools: Read, Grep, Glob, Write, Edit
user-invocable: true
metadata:
  author: Forgather
  version: 1.0.0
---

# Test Writer 스킬

이 스킬은 테스트 코드 생성 시 참조하는 전문 지식입니다.

## 사용법

```
/test-writer {타입} {대상클래스}
```

- **타입**: `unit` | `service` | `acceptance`
- **대상클래스**: 테스트할 클래스 이름 (예: `SpaceService`, `Product`)

## Examples

```
/test-writer unit Space
/test-writer service SpaceService
/test-writer acceptance Space
```

## 생성 가능한 테스트 타입

| Type | 설명 | 상속/어노테이션 |
|------|------|----------------|
| unit | 단위 테스트 | 없음 (순수 JUnit5) |
| service | 서비스 통합 테스트 | `TestOnContainer` + `@SpringBootTest` |
| acceptance | 인수 테스트 | `AcceptanceTest` + `RestAssuredMockMvc` |

## 생성 프로세스

1. 테스트 대상 클래스 분석 (필드, 메서드, 의존성)
2. 테스트 타입 결정 (unit/service/acceptance)
3. 기존 Fixture 확인 (`src/test/java/com/forgather/fixture/`)
4. 기존 Fake 구현체 확인 (`src/test/java/com/forgather/fake/`)
5. 이 스킬의 `assets/` 하위 템플릿 참조하여 코드 생성

## 템플릿 파일

- `assets/unit.java.template` - 단위 테스트 기본 구조
- `assets/service.java.template` - 서비스 통합 테스트 구조
- `assets/acceptance.java.template` - 인수 테스트 구조

---

## 컨벤션 요약

- `@DisplayName`: 동작 + 결과, 한글 작성
- 메서드명: camelCase, 동작 중심
- 구조: given-when-then 주석 필수
- 상세 가이드: `references/conventions.md`

---

## 테스트 타입별 가이드

### 1. 단위 테스트 (Unit Test)

**대상**: 엔티티, 값 객체, 유틸리티 클래스

**특징**:
- 순수 JUnit5만 사용 (Spring 컨텍스트 불필요)
- 외부 의존성 없음
- 빠른 실행 속도

**필수 어노테이션**:
```java
@DisplayName("테스트 설명")
@Test
```

**예시**: `SpaceTest`, `ProductTest`, `GuestBookCardTest`

### 2. 서비스 통합 테스트 (Service Test)

**대상**: Service 클래스

**특징**:
- `TestOnContainer` 상속 (TestContainers 사용)
- 실제 DB 연동 테스트
- 트랜잭션 롤백으로 데이터 격리

**필수 어노테이션**:
```java
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
```

**의존성 주입**:
```java
private final SpaceService spaceService;
private final SpaceRepository spaceRepository;

@Autowired
public SpaceServiceTest(SpaceService spaceService, SpaceRepository spaceRepository) {
    this.spaceService = spaceService;
    this.spaceRepository = spaceRepository;
}
```

**예시**: `SpaceServiceTest`, `AdminHostServiceTest`

### 3. 인수 테스트 (Acceptance Test)

**대상**: Controller (API 엔드포인트)

**특징**:
- `AcceptanceTest` 상속
- RestAssuredMockMvc 사용
- 실제 HTTP 요청/응답 테스트
- `@Sql`로 테스트 후 데이터 정리

**필수 어노테이션**:
```java
@DisplayName("인수 테스트: {Domain}")
@AutoConfigureMockMvc
```

**MockMvc 설정**:
```java
@Autowired
private MockMvc mockMvc;

@BeforeEach
void setUp() throws IOException {
    RestAssuredMockMvc.mockMvc(mockMvc);
}
```

**외부 의존성 Mock**:
```java
@MockitoBean
private ContentsStorage contentsStorage;

@BeforeEach
void setUp() {
    Mockito.when(contentsStorage.upload(any(), any()))
        .thenReturn("forgather/temp.png");
}
```

**예시**: `SpaceAcceptanceTest`, `ProductAcceptanceTest`

---

## 상세 참조 문서

- `references/conventions.md` - @DisplayName, 메서드명, given-when-then 상세 코드 예제
- `references/fixture-guide.md` - Fixture 사용 원칙, Fake 구현체 활용법
- `references/restassured-patterns.md` - GET/POST/DELETE/Multipart 요청 패턴
- `references/assertion-patterns.md` - 단일/다중/예외 검증 패턴
- `references/import-guide.md` - 테스트 타입별 Import 목록
- `references/troubleshooting.md` - 자주 발생하는 문제와 해결법
