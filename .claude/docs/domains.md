# 도메인 모델

## 핵심 엔티티 관계

```
Host (작가)
  ├── HostProfilePhoto (프로필 사진) 1:1
  └── Space (전시 공간) 1:1
        ├── Product (작품) 1:N
        │     └── ProductPhoto 1:N
        └── GuestBookCard (방명록) 1:N
              ├── Guest (방문객) N:1
              └── GuestBookCardPhoto 1:N
```

## Space (전시 공간)

작가가 운영하는 온라인 전시 공간

| 필드 | 설명 | 제약조건 |
|------|------|----------|
| code | 고유 코드 | 10자 고정 |
| name | 공간 이름 | 최대 30자 |
| description | 설명 | 최대 200자 |
| isPublic | 공개 여부 | - |
| instagramUsername | 인스타그램 | 최대 30자 |
| email | 연락처 | 최대 50자 |

## Product (작품)

전시 공간에 등록된 작품

- Space와 N:1 관계
- ProductPhoto를 일급 컬렉션(ProductPhotos)으로 관리
- Soft Delete 적용

## GuestBook (방명록)

### Guest (방문객)
방명록 작성자 정보
- nickname, password 관리
- Soft Delete 적용

### GuestBookCard (방명록 카드)
실제 방명록 내용
- Space, Guest와 연관
- GuestBookCardPhoto를 일급 컬렉션으로 관리
- Soft Delete 적용

## 값 객체 (Value Objects)

### Photo 관련
```java
// 공통 사진 매핑 상위 클래스 - SoftDeleteEntity 상속
@MappedSuperclass
public abstract class Photo extends SoftDeleteEntity {
    protected Long id;
    protected String originalName;  // presigned 업로드 흐름에서는 "" 로 둔다
    protected String path;          // S3 key. 응답도 이 값을 내려주고 프론트가 CDN URL을 조립한다
    protected Long capacity;        // bytes
}
```

구현체는 모두 자식이 FK를 소유한다:

| 엔티티 | 관계 | 비고 |
|--------|------|------|
| `SpacePhoto` | Space 1:1 | multipart 업로드, `empty()` 널 오브젝트 사용 |
| `ProductPhoto` | Product N:1 | `sortOrder` 보유 |
| `GuestBookCardPhoto` | GuestBookCard N:1 | |
| `ExhibitionPhoto` | Exhibition 1:1 | presigned 업로드, `originalName = ""` |
| `HostProfilePhoto` | Host 1:1 | presigned 업로드, `originalName = ""` |

soft delete 때문에 같은 부모로 여러 행(삭제된 행 N + 활성 행 1)이 존재하므로, 1:1이어도 FK 컬럼에 unique를 걸지 않는다.
단 H2(`ddl-auto: create-drop`) 기반 테스트에서는 Hibernate가 `@OneToOne` FK에 unique를 자동 생성하므로,
사진 교체를 검증하는 테스트는 `TestOnContainer`(MySQL + Flyway) 위에서 실행해야 한다.

### ProductPhotos / GuestBookCardPhotos
사진 목록을 관리하는 일급 컬렉션:
- 사진 개수 제한 검증
- 순서(orderIndex) 관리
- 추가/삭제 비즈니스 규칙 캡슐화

## 데이터 검증 전략

### 엔티티 레벨 검증
- 생성자에서 필수값 및 형식 검증
- `TextLengthCounter`로 한글 포함 문자열 길이 계산
- 검증 실패 시 `BaseException` 발생

### RequiredFields 패턴
NOT NULL 컬럼에 해당하는 필드들은 `validateRequiredFields()`로 한 번에 null 검증:
```java
private void validateRequiredFields(String code, String name, ...) {
    if (code == null) {
        throw new BaseNullPointerException("스페이스 코드는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
    if (name == null) {
        throw new BaseNullPointerException("스페이스 이름은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
    // ... 모든 NOT NULL 필드 검증
}
```
- 생성자 첫 줄에서 호출
- 각 필드별 형식 검증(`validateName()` 등)은 그 다음에 수행

### DTO 레벨 검증
- Jakarta Validation 어노테이션 사용
- `@Valid`로 컨트롤러에서 검증

### 검증 예시
```java
// Space 생성자 내 검증
private void validateName(String name) {
    if (name.isBlank()) {
        throw new BaseException("스페이스 이름은 공백만 입력할 수 없습니다.");
    }
    if (TextLengthCounter.count(name) > MAX_NAME_LENGTH) {
        throw new BaseException("스페이스 이름은 최대 %d자까지 가능합니다.");
    }
}
```

## 상태 관리

### Soft Delete 상태
- `deletedAt == null`: 활성 상태
- `deletedAt != null`: 삭제됨

### Space 공개 상태
- `isPublic = true`: 검색 및 목록에 노출
- `isPublic = false`: 직접 접근만 가능
