# JPA 감지 패턴 카탈로그

`jpa-analyzer` 에이전트가 코드에서 찾아야 할 패턴. 각 패턴은 **문제 코드 예시**, **감지 정규식**, 대응하는 **해결 패턴** 링크를 포함한다.

## N+1 패턴 (실제 발생 가능성 순)

### Pattern 1 — 루프 내 Repository 호출 (🔴 HIGH)

가장 명확한 N+1. 반복마다 쿼리가 실행된다.

```java
// 🔴 for-each 루프
for (Product product : products) {
    repository.findByProduct(product);  // 매 반복마다 쿼리 발생!
}

// 🔴 Stream에서 Repository 호출
products.stream()
    .map(p -> repository.findBy(p))  // 매 요소마다 쿼리 발생!
    .toList();
```

**감지 정규식**
- `for (... : ...) { .*repository.*find`
- `forEach.*repository.*find`
- `stream().*map.*repository.*find`
- `stream().*flatMap.*repository.*find`

**해결** → [배치 쿼리](./04-batch-query.md) (IN 절 + Map) 또는 [Fetch Join](./01-fetch-join.md)

---

### Pattern 2 — 중첩 서비스 호출 (🟡 MEDIUM)

다른 서비스 메서드가 내부에서 Repository를 호출할 수 있어 간접 N+1이 된다.

```java
// 🟡 잠재적 N+1 - deleteCard 내부에서 쿼리 발생?
for (Card card : cards) {
    deleteCard(card.getId());
}
```

**감지 정규식**
- `for (... : ...) { .*Service\.`
- `forEach.*Service\.`

**해결** → 루프 대신 서비스 메서드에 컬렉션을 받는 배치 API를 추가. 상세는 [배치 쿼리](./04-batch-query.md).

---

### Pattern 3 — Lazy Loading 컬렉션 접근 (🟡 MEDIUM)

Lazy 컬렉션이 루프 내에서 초기화되면 각각 쿼리가 발생한다.

```java
// 🟡 Lazy 컬렉션 루프 내 접근
for (Order order : orders) {
    order.getItems().size();  // Lazy 컬렉션 초기화!
}
```

**감지 정규식**
- `for.*get.*\(\)\.` (컬렉션 getter 호출 패턴)

**해결** → [Fetch Join](./01-fetch-join.md) 또는 [EntityGraph](./02-entity-graph.md)

---

## 최적화 기회 (적용 시 유의미한 이득)

### Fetch Join 미사용 (🟢 LOW)

연관 엔티티를 함께 조회해야 하는데 Fetch Join 없이 쿼리가 작성된 경우.

**좋은 예시 (Forgather 실제 코드 — `SpaceHostMapRepository.java:20-28`)**

```java
@Query("""
    SELECT shm
    FROM SpaceHostMap shm
    JOIN FETCH shm.space s
    WHERE shm.host = :host
    AND s.deletedAt IS NULL
    """)
List<SpaceHostMap> findAllByHostWithSpace(@Param("host") Host host);
```

**해결** → [Fetch Join](./01-fetch-join.md)

---

### DTO 프로젝션 전환 기회 (🟢 LOW)

전체 엔티티를 조회하고 일부 필드만 사용하는 경우, DTO 프로젝션으로 로딩량을 줄일 수 있다.

**좋은 예시 (`GuestBookCardRepository.java:38-51`)**

```java
@Query("""
    SELECT new com.forgather.domain.guestbook.repository.dto.GuestBookCardListDto(
        g.id,
        guest.nickname,
        g.isRead,
        CASE WHEN (SELECT COUNT(p) FROM GuestBookCardPhoto p WHERE p.guestBookCard = g) > 0
             THEN true ELSE false END
    )
    FROM GuestBookCard g
    JOIN g.guest guest
    WHERE g.space = :space AND g.deletedAt IS NULL
    """)
Page<GuestBookCardListDto> findAllDtoBySpace(@Param("space") Space space, Pageable pageable);
```

**해결** → [DTO 프로젝션](./03-dto-projection.md)

---

### 배치 쿼리 적용 기회 (🟡 MEDIUM)

루프 바깥에서 ID 목록으로 IN 쿼리 한 번에 조회 후 Map으로 매핑해 O(1) 룩업.

**좋은 예시 (`SpaceService.java:181-208`)**

```java
private List<SpaceResponse> createSpaceResponses(List<SpaceHostMap> spaceHostMaps) {
    List<Long> spaceIds = spaceHostMaps.stream()
        .map(shm -> shm.getSpace().getId())
        .toList();

    // 배치 쿼리로 한 번에 조회
    Map<Long, Long> guestBookCardCounts = guestBookCardRepository
        .countBySpaceIdAndDeletedAtIsNullIn(spaceIds)
        .stream()
        .collect(Collectors.toMap(
            SpaceGuestBookCountDto::spaceId,
            SpaceGuestBookCountDto::guestBookCount));

    Map<Long, SpacePhoto> spacePhotos = spacePhotoRepository
        .findAllBySpaceIdInAndDeletedAtIsNull(spaceIds)
        .stream()
        .collect(Collectors.toMap(
            photo -> photo.getSpace().getId(),
            photo -> photo));

    return spaceHostMaps.stream()
        .map(shm -> {
            Space space = shm.getSpace();
            Long count = guestBookCardCounts.getOrDefault(space.getId(), 0L);
            SpacePhoto photo = spacePhotos.getOrDefault(space.getId(), SpacePhoto.empty(space));
            return SpaceResponse.from(space, photo, count);
        })
        .toList();
}
```

**해결** → [배치 쿼리](./04-batch-query.md)

---

## 감지 요약표

| # | 패턴 | 심각도 | 핵심 감지 단서 | 해결 |
|---|---|---|---|---|
| 1 | 루프 내 Repository 호출 | 🔴 HIGH | `for/forEach/stream` 안의 `repository.find*` | 배치 / Fetch Join |
| 2 | 중첩 서비스 호출 | 🟡 MEDIUM | `for/forEach` 안의 `*Service.*()` | 배치 API 추가 |
| 3 | Lazy 컬렉션 루프 접근 | 🟡 MEDIUM | `for` 안의 `get*().*` | Fetch Join / EntityGraph |
| 4 | Fetch Join 미사용 | 🟢 LOW | 연관 조회인데 `JOIN FETCH` 없음 | Fetch Join |
| 5 | DTO 프로젝션 기회 | 🟢 LOW | 엔티티 전체 조회 후 필드 일부만 사용 | DTO 프로젝션 |
| 6 | 배치 쿼리 기회 | 🟡 MEDIUM | 루프 + 개별 ID 조회 | 배치 쿼리 |
