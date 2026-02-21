# Fixture & Fake 구현체 가이드

## Fixture 사용 원칙

### 기존 Fixture 활용
테스트 데이터는 `src/test/java/com/forgather/fixture/` 의 Fixture 클래스 활용:

```java
Space space = SpaceFixture.createSpace();
Host host = HostFixture.createHost();
Space spaceWithCode = SpaceFixture.createSpaceWithCode("abcdefghij");
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
