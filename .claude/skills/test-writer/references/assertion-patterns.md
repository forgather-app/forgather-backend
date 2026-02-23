# Assertion 패턴

## 단일 검증
```java
assertThat(result.getName()).isEqualTo("expected");
```

## 다중 검증 (assertAll)
```java
assertAll(
    () -> assertThat(result.name()).isEqualTo("새로운 스페이스"),
    () -> assertThat(result.description()).isEqualTo("새로운 설명"),
    () -> assertThat(result.isPublic()).isFalse()
);
```

## 예외 검증
```java
assertThatThrownBy(
    () -> new Space(null, name, null, false, null, null)
).isInstanceOf(BaseNullPointerException.class)
    .hasMessageContaining("스페이스 코드");
```

## 예외 없음 검증
```java
assertThatCode(
    () -> new Space(spaceCode, name, "", false, "", "")
).doesNotThrowAnyException();
```
