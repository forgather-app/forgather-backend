# RestAssuredMockMvc 패턴

## GET 요청

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

## POST 요청 (JSON)

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

## POST 요청 (Multipart)

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

## DELETE 요청

```java
var response = RestAssuredMockMvc.given()
    .header("Authorization", "Bearer " + token)
    .when()
    .delete("/spaces/{spaceCode}", space.getCode())
    .then()
    .extract();

assertThat(response.statusCode()).isEqualTo(204);
```

## 인증 없는 요청 테스트

```java
var response = RestAssuredMockMvc.given()
    .when()  // Authorization 헤더 없음
    .delete("/spaces/{spaceCode}", space.getCode())
    .then()
    .extract();

assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
```
