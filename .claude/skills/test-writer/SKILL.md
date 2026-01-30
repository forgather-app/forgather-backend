---
name: test-writer
description: 테스트 코드 생성 요청 시 사용. unit/service/acceptance 테스트 작성.
allowed-tools: Read, Grep, Glob, Write, Edit
user-invocable: true
---

# Test Writer 스킬

이 스킬은 테스트 코드 생성 시 참조하는 전문 지식입니다.

## 사용법

```
/test-writer {타입} {대상클래스}
```

- **타입**: `unit` | `service` | `acceptance`
- **대상클래스**: 테스트할 클래스 이름 (예: `SpaceService`, `Product`)

## 예시

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
5. 이 스킬의 `templates/` 하위 템플릿 참조하여 코드 생성

## 템플릿 파일

- `templates/unit.java.template` - 단위 테스트 기본 구조
- `templates/service.java.template` - 서비스 통합 테스트 구조
- `templates/acceptance.java.template` - 인수 테스트 구조

---

## 테스트 컨벤션

### @DisplayName 작성 원칙

- **동작 + 결과** 형태로 작성
- 도메인 요구사항을 반영하여 한글로 작성
- 예외 케이스는 조건 + 결과로 명시

```java
// Good
@DisplayName("스페이스 생성에 코드와 이름은 필수값이다.")
@DisplayName("스페이스 이름이 존재하지 않으면 스페이스를 생성할 수 없다.")
@DisplayName("논리 삭제된 스페이스를 조회하면 예외를 던진다")

// Bad
@DisplayName("test1")
@DisplayName("createSpace")
```

### 메서드명 규칙

- **카멜케이스** 사용
- 동작 중심으로 작성
- 예외 케이스는 `Without`, `With` 접미사 활용

```java
// Good
void createSpace() { }
void createSpaceWithoutLogin() { }
void createSpaceWithInvalidName() { }

// Bad
void test_create_space() { }
void testCreateSpace() { }
```

### given-when-then 구조

모든 테스트는 given-when-then 주석으로 구조화:

```java
@Test
void createSpace() {
    // given
    String spaceCode = "1234567890";
    String name = "나의 졸업전시";

    // when
    Space space = new Space(spaceCode, name, "", false, "", "");

    // then
    assertThat(space.getName()).isEqualTo(name);
}
```

`when & then`을 함께 쓰는 경우 (예외 검증):

```java
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

## RestAssuredMockMvc 패턴

### GET 요청
```java
SpaceResponse result = RestAssuredMockMvc.given()
    .header("Authorization", "Bearer " + token)
    .when()
    .get("/spaces/{spaceCode}", space.getCode())
    .then()
    .statusCode(HttpStatus.OK.value())
    .extract()
    .body()
    .as(SpaceResponse.class);
```

### POST 요청 (JSON)
```java
CreateResponse response = RestAssuredMockMvc.given()
    .header("Authorization", "Bearer " + token)
    .contentType(ContentType.JSON)
    .body(request)
    .when()
    .post("/endpoint")
    .then()
    .statusCode(HttpStatus.CREATED.value())
    .extract()
    .body()
    .as(CreateResponse.class);
```

### POST 요청 (Multipart)
```java
MockMultipartFile file = new MockMultipartFile(
    "file", "test.jpg", "image/jpeg", "content".getBytes()
);
String request = objectMapper.writeValueAsString(createRequest);

CreateSpaceResponse response = RestAssuredMockMvc.given()
    .header("Authorization", "Bearer " + token)
    .multiPart("request", request, "application/json")
    .multiPart("file", file.getOriginalFilename(), file.getBytes(), file.getContentType())
    .when()
    .post("/spaces")
    .then()
    .statusCode(HttpStatus.CREATED.value())
    .extract()
    .body()
    .as(CreateSpaceResponse.class);
```

### DELETE 요청
```java
var response = RestAssuredMockMvc.given()
    .header("Authorization", "Bearer " + token)
    .when()
    .delete("/spaces/{spaceCode}", space.getCode())
    .then()
    .extract();

assertThat(response.statusCode()).isEqualTo(204);
```

### 인증 없는 요청 테스트
```java
var response = RestAssuredMockMvc.given()
    .when()  // Authorization 헤더 없음
    .delete("/spaces/{spaceCode}", space.getCode())
    .then()
    .extract();

assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
```

---

## Fixture 사용 원칙

### 기존 Fixture 활용
테스트 데이터는 `src/test/java/com/forgather/fixture/` 의 Fixture 클래스 활용:

```java
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.HostFixture;

// 기본 생성
Space space = SpaceFixture.createSpace();
Host host = HostFixture.createHost();

// 커스텀 생성
Space space = SpaceFixture.createSpaceWithCode("abcdefghij");
```

### 새 Fixture 추가 시
같은 패턴으로 정적 팩토리 메서드 제공:

```java
public class NewEntityFixture {

    public static NewEntity createEntity() {
        return new NewEntity("default", "values");
    }

    public static NewEntity createEntityWith{Condition}({Type} param) {
        return new NewEntity(param, "values");
    }
}
```

---

## Fake 구현체 활용

**원칙**: 테스트에서 실제 외부 API 호출 금지

```java
// src/test/java/com/forgather/fake/FakeContentStorage.java
public class FakeContentStorage implements ContentsStorage {
    @Override
    public String upload(String spaceCode, MultipartFile file) {
        return "";  // 실제 S3 업로드하지 않음
    }

    @Override
    public String issueSignedUrl(String path) {
        return "test-prefix-" + path + "-test-suffix";
    }
}
```

인수 테스트에서는 `@MockitoBean`으로 Mock 처리:
```java
@MockitoBean
private ContentsStorage contentsStorage;
```

---

## Assertion 패턴

### 단일 검증
```java
assertThat(result.getName()).isEqualTo("expected");
```

### 다중 검증 (assertAll)
```java
assertAll(
    () -> assertThat(result.name()).isEqualTo("새로운 스페이스"),
    () -> assertThat(result.description()).isEqualTo("새로운 설명"),
    () -> assertThat(result.isPublic()).isFalse()
);
```

### 예외 검증
```java
assertThatThrownBy(
    () -> new Space(null, name, null, false, null, null)
).isInstanceOf(BaseNullPointerException.class)
    .hasMessageContaining("스페이스 코드");
```

### 예외 없음 검증
```java
assertThatCode(
    () -> new Space(spaceCode, name, "", false, "", "")
).doesNotThrowAnyException();
```

---

## Import 가이드

### 단위 테스트
```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
```

### 서비스 테스트
```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
```

### 인수 테스트
```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.acceptance.AcceptanceTest;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
```
