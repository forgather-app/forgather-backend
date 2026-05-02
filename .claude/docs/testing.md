# 테스트 전략

## 테스트 구조

```
src/test/java/com/forgather/
├── acceptance/    # 인수 테스트 (E2E)
├── domain/        # 단위 테스트
├── back_office/   # 관리자 기능 테스트
├── container/     # TestContainers 설정
├── fixture/       # 테스트 픽스처
├── fake/          # Fake 구현체
└── util/          # 테스트 유틸리티
```

## DB 선택 가이드: H2 vs TestContainers

테스트 유형에 따라 적절한 DB를 선택합니다.

| 테스트 유형 | DB 선택 | 사용 시점 |
|------------|---------|----------|
| 슬라이스 테스트 (`@DataJpaTest`) | H2 인메모리 | 가볍고 빠른 테스트 필요 시 |
| 인수 테스트 (`AcceptanceTest`) | TestContainers MySQL | 운영 환경과 동일한 DB 필요 시 |
| 서비스 통합 테스트 | TestContainers MySQL | 실제 DB 동작 검증 필요 시 |

### TestContainers 설정

MySQL 8.0 컨테이너를 사용한 통합 테스트:

```java
// TestOnContainer.java
protected static final MySQLContainer mysql =
    new MySQLContainer("mysql:8.0.42")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
```

#### 특징
- JVM 종료 시 컨테이너 자동 정리
- Flyway 마이그레이션 자동 적용
- 테스트 간 데이터 격리 (`cleanup.sql`)

### H2 인메모리 설정 (슬라이스 테스트)

```java
@Import({ProductService.class, FakeContentStorage.class})
@DataJpaTest
public class ProductDeleteServiceTest {
    // H2 인메모리 DB 사용, 가볍고 빠름
}
```

## 인수 테스트 (Acceptance Test)

### 기본 구조
```java
@ActiveProfiles("test")
@Sql(scripts = "/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class AcceptanceTest extends TestOnContainer {
    @LocalServerPort
    int port;
}
```

### RestAssuredMockMvc 패턴

프로젝트에서는 `RestAssuredMockMvc`를 사용합니다.

```java
@DisplayName("스페이스를 상세 조회한다.")
@Test
void getSpaceInformation() {
    // given
    Space space = spaceRepository.save(SpaceFixture.createSpace());
    SpacePhoto spacePhoto = spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
    SpaceHostRepository.save(new SpaceHost(space, host));

    // when
    SpaceResponse result = RestAssuredMockMvc.given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/spaces/{spaceCode}", space.getCode())
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .body()
        .as(SpaceResponse.class);

    // then
    assertAll(
        () -> assertThat(result.spaceCode()).isEqualTo(space.getCode()),
        () -> assertThat(result.spacePhoto().path()).isEqualTo(spacePhoto.getPath())
    );
}
```

## 테스트 픽스처

### fixture/ 패키지
테스트 데이터 생성을 위한 빌더 패턴:
- 일관된 테스트 데이터 생성
- 필요한 필드만 커스터마이징
- 기본값으로 유효한 객체 생성

## Fake 구현

### fake/ 패키지
외부 의존성을 대체하는 Fake 객체:
- S3 업로드: 실제 S3 호출 대신 메모리 저장
- OAuth: 실제 카카오 API 대신 가짜 응답

### 사용 원칙
- 테스트에서 실제 외부 API 호출 금지
- `@Profile("test")`로 테스트 환경에서만 활성화

## 테스트 실행

### 전체 테스트
```bash
./gradlew test
```

### 특정 테스트 클래스
```bash
./gradlew test --tests "SpaceServiceTest"
```

### 특정 테스트 메서드
```bash
./gradlew test --tests "SpaceServiceTest.getSpaceInformation"
```

## 테스트 작성 가이드

### 네이밍 컨벤션

메서드명은 **카멜케이스**로 간결하게 작성합니다.

**예시**:
- `createSpace()` - 기본 성공 케이스
- `createSpaceWithoutLogin()` - 인증 실패 케이스
- `spaceNameValidationTest()` - 검증 테스트

### @DisplayName 작성 원칙

**3가지 원칙**:
1. **동작과 결과 모두 기술**: "~하면 ~된다" 형식
2. **도메인 요구사항 반영**: 비즈니스 규칙을 명확히 표현
3. **관계 명확화**: 도메인 간 관계가 있으면 함께 기술

**좋은 예시**:
- "스페이스 코드가 존재하지 않으면 스페이스를 생성할 수 없다."
- "논리 삭제된 스페이스를 조회하면 예외를 던진다"
- "스페이스를 상세 조회한다."

**나쁜 예시**:
- "스페이스 생성 테스트" (결과 미기술)
- "테스트1" (의미 없음)

### 단위 테스트

외부 의존성 없이 순수 로직을 테스트합니다.

```java
@DisplayName("스페이스 코드가 존재하지 않으면 스페이스를 생성할 수 없다.")
@Test
void createSpaceWithoutCode() {
    // given
    String name = "나의 졸업전시";

    // when & then
    assertThatThrownBy(
        () -> new Space(null, name, null, false, null, null)
    ).isInstanceOf(BaseNullPointerException.class)
        .hasMessageContaining("스페이스 코드");
}
```

### 서비스 통합 테스트

Service 레이어를 Repository와 함께 실제 DB로 테스트합니다.

```java
@DisplayName("논리 삭제된 스페이스를 조회하면 예외를 던진다")
@Test
void shouldThrowExceptionWhenQuerySoftDeletedSpace() {
    // given
    Host host = userRepository.save(HostFixture.createHost());
    Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode("abcdefghij"));
    SpaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space, host));
    spaceService.delete(space.getCode(), host);

    // when & then
    assertThatThrownBy(() -> spaceService.getSpaceInformation(space.getCode()))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("존재하지 않는 스페이스입니다.");
}
```

### 인수 테스트

API 엔드포인트를 E2E로 테스트합니다. 사용자 시나리오 기반으로 작성합니다.
