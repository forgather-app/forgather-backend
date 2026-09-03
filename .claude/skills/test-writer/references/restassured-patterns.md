# RestAssuredMockMvc 패턴

## 인증

호스트 인증은 `access_token` HttpOnly 쿠키로만 이루어진다. 인수 테스트에서는 `AcceptanceTest`의
`withAccessToken(token)` / `withRefreshToken(token)` 헬퍼를 `.postProcessors(...)`에 넘긴다.
RestAssuredMockMvc의 `.cookie()`는 Spring 6.2의 bridge 메서드 때문에 JVM 실행에 따라
"argument type mismatch"로 실패하므로 사용하지 않는다.

## GET 요청

```java
SpaceResponse result = RestAssuredMockMvc.given()
    .postProcessors(withAccessToken(token))
    .when()
    .get("/spaces/{spaceCode}", space.getCode())
    .then()
    .statusCode(HttpStatus.OK.value())
    .extract()
    .body()
    .as(SpaceResponse.class);
```

## POST 요청 (JSON)

```java
CreateResponse response = RestAssuredMockMvc.given()
    .postProcessors(withAccessToken(token))
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

## POST 요청 (Multipart)

```java
MockMultipartFile file = new MockMultipartFile(
    "file", "test.jpg", "image/jpeg", "content".getBytes()
);
String request = objectMapper.writeValueAsString(createRequest);

CreateSpaceResponse response = RestAssuredMockMvc.given()
    .postProcessors(withAccessToken(token))
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

## DELETE 요청

```java
var response = RestAssuredMockMvc.given()
    .postProcessors(withAccessToken(token))
    .when()
    .delete("/spaces/{spaceCode}", space.getCode())
    .then()
    .extract();

assertThat(response.statusCode()).isEqualTo(204);
```

## 실패 응답 검증

모든 4xx/5xx 응답은 `{code, message, data: null}` envelope 포맷이다. 인수 테스트에서는 `code` 키를 명시적으로 검증해 envelope 회귀를 막는다. `.extract()` 후 외부 `assertThat`으로 status를 검증하는 구식 패턴 대신 `.then()` 체이닝에 `.statusCode(...)` + `.body("code", equalTo(...))`를 함께 둔다.

```java
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
```

### 인증 실패 (access_token 쿠키 없음)

```java
RestAssuredMockMvc.given()
    .when()  // access_token 쿠키 없음
    .delete("/spaces/{spaceCode}", space.getCode())
    .then()
    .statusCode(HttpStatus.UNAUTHORIZED.value())
    .body("code", equalTo("UNAUTHORIZED"));
```

### 권한/리소스/검증 실패 (메시지 포함)

도메인 메시지를 함께 검증하는 케이스. `code`는 envelope 회귀 가드, `message`는 도메인 동작 검증 역할.

```java
RestAssuredMockMvc.given()
    .postProcessors(withAccessToken(token))
    .when()
    .delete("/spaces/{spaceCode}/guestbook/{cardId}", spaceCode, cardId)
    .then()
    .statusCode(403)
    .body("code", equalTo("FORBIDDEN"))
    .body("message", containsString("해당 스페이스에 대한 접근 권한이 없습니다."));
```

### `code` 매핑 빠른 참조

| HTTP | code | 비고 |
|---|---|---|
| 400 | `BAD_REQUEST` | `BaseException`(default), `MultipartException`, 타입 불일치 등 |
| 400 | `VALIDATION_FAILED` | `@Valid` 실패 (`MethodArgumentNotValidException`) |
| 401 | `UNAUTHORIZED` | `UnauthorizedException`, 세션/인증 누락 |
| 401 | `JWT_INVALID` | `JwtException` 계열 |
| 403 | `FORBIDDEN` | `ForbiddenException`, 권한 부족 |
| 404 | `NOT_FOUND` | 커스텀 `NotFoundException` |
| 404 | `RESOURCE_NOT_FOUND` | Spring `NoResourceFoundException` (정적 리소스) |
| 409 | `CONFLICT` | `ConflictException` |
| 5xx | `INTERNAL_ERROR` / `FILE_*_FAILED` | 메시지는 마스킹되므로 `containsString` 금지 |

> 5xx는 응답 메시지가 영역별 고정 문구로 마스킹된다(`"예상치 못한 오류가 발생했습니다."` 등). 도메인 원본 메시지는 인수 테스트에서 검증할 수 없으니 `code`로만 분기한다.
