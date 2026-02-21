# 테스트 컨벤션 상세 가이드

## @DisplayName 작성 원칙

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

## 메서드명 규칙

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

## given-when-then 구조

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
