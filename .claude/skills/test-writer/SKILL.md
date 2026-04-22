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

## When to Activate

다음 요청이나 키워드가 나타날 때 활성화:

```
/test-writer {타입} {대상클래스}
```

| 타입 | 대상 |
|---|---|
| `unit` | 엔티티, 값 객체 |
| `service` | Service 클래스 |
| `acceptance` | Controller (API) |

**자동 트리거**
- "테스트 작성해줘", "test 코드 만들어줘"
- "단위/서비스/인수 테스트 추가"
- JUnit5, RestAssured, TestFixture, @DisplayName 언급
- `*Test.java` 파일 작성 컨텍스트

**명시 호출 예시**
```
/test-writer unit Space
/test-writer service SpaceService
/test-writer acceptance Space
```

## Core Concepts

### 테스트 타입 3계층

| 타입 | 목적 | 상속 / 어노테이션 | 속도 | 외부 의존 |
|---|---|---|---|---|
| **unit** | 엔티티 비즈니스 규칙 검증 | 순수 JUnit5 | 빠름 | 없음 |
| **service** | Service + DB 연동 검증 | `TestOnContainer` + `@SpringBootTest` | 보통 | TestContainers MySQL |
| **acceptance** | API 엔드포인트 전체 흐름 검증 | `AcceptanceTest` + RestAssuredMockMvc | 보통~느림 | Mockito + Fake |

### 핵심 원칙
- **테스트 간 격리**: `cleanup.sql` 적용, 공유 상태 금지
- **외부 API 금지**: 실제 Kakao / S3 호출 대신 Fake 구현체 사용
- **Given-When-Then 구조**: 3블록 주석 필수
- **@DisplayName**: 동작 + 결과를 한글 서술

## Code Examples

### Unit Test 기본 구조

```java
@DisplayName("스페이스 코드가 중복되면 생성할 수 없다")
@Test
void 스페이스_코드_중복_생성_실패() {
    // given
    Space existing = SpaceFixture.create("abc123");

    // when & then
    assertThatThrownBy(() -> new Space("abc123", ...))
        .isInstanceOf(DuplicateSpaceCodeException.class);
}
```

### Service Test (통합)

```java
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Transactional
class SpaceServiceTest extends TestOnContainer {

    private final SpaceService spaceService;
    private final SpaceRepository spaceRepository;

    @Autowired
    public SpaceServiceTest(SpaceService spaceService, SpaceRepository spaceRepository) {
        this.spaceService = spaceService;
        this.spaceRepository = spaceRepository;
    }
}
```

### Acceptance Test (API)

```java
@DisplayName("인수 테스트: Space")
@AutoConfigureMockMvc
class SpaceAcceptanceTest extends AcceptanceTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ContentsStorage contentsStorage;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        Mockito.when(contentsStorage.upload(any(), any()))
            .thenReturn("forgather/temp.png");
    }
}
```

**상세 import / RestAssured 패턴 / assertion 패턴**: `references/` 하위 참조

## Best Practices

- `@DisplayName`: 동작 + 결과를 한글 서술 (예: `"스페이스 코드가 존재하지 않으면 스페이스를 생성할 수 없다"`)
- 메서드명: camelCase, 동작 중심 네이밍
- 구조: `// given`, `// when`, `// then` 주석 필수
- 외부 의존은 `@MockitoBean` + `Mockito.when()` 또는 Fake 구현체로 대체
- 테스트 데이터는 Fixture로 생성 (직접 `new Entity(...)` 지양)

## Checklist

테스트 작성 후 확인:

- [ ] 상속 클래스가 테스트 타입에 맞는가? (unit=없음, service=TestOnContainer, acceptance=AcceptanceTest)
- [ ] `@DisplayName`이 **행위 + 결과**를 모두 서술하는가?
- [ ] given-when-then 주석 블록이 있는가?
- [ ] 외부 API 호출을 Fake/MockitoBean으로 대체했는가?
- [ ] acceptance 테스트에 `@Sql("cleanup.sql")` 또는 `@Transactional` 격리가 적용되었는가?
- [ ] Fixture가 재사용 가능한 형태로 분리되었는가? (`src/test/java/com/forgather/fixture/`)
- [ ] 예외 케이스 테스트가 포함되었는가?

## Red Flags

| 안티패턴 | 해결 |
|---|---|
| 실제 Kakao/S3 API 호출 | Fake 구현체 주입 (`src/test/java/com/forgather/fake/`) |
| `cleanup.sql` 누락 → 테스트 간 데이터 오염 | acceptance 테스트에 `@Sql` 또는 `@Transactional` 적용 |
| `@DisplayName("스페이스 테스트")` 같은 모호한 설명 | 행위+결과를 한 문장으로 서술 |
| 엔티티를 `new`로 만들고 필수 필드 빠뜨림 | `SpaceFixture.create(...)` 같은 Fixture 팩토리 사용 |
| `given` 없이 테스트 안에서 복잡한 set-up | @BeforeEach + Fixture로 이동 |
| 여러 assertion이 하나의 테스트에 섞여 있음 | 하나의 동작 = 하나의 테스트로 분리 |

## 상세 참조

- `references/conventions.md` — @DisplayName, 메서드명, given-when-then 상세 예제
- `references/fixture-guide.md` — Fixture 원칙, Fake 구현체 활용
- `references/restassured-patterns.md` — GET/POST/DELETE/Multipart 요청 패턴
- `references/assertion-patterns.md` — 단일/다중/예외 검증 패턴
- `references/import-guide.md` — 테스트 타입별 Import 목록
- `references/troubleshooting.md` — 자주 발생하는 문제와 해결법

## 템플릿

- `assets/unit.java.template` — 단위 테스트
- `assets/service.java.template` — 서비스 통합 테스트
- `assets/acceptance.java.template` — 인수 테스트
