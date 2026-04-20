# 배치 쿼리 — IN 절로 N+1 해결

**문제**: 루프 내에서 개별 ID로 조회하여 N+1 발생
**해결**: IN 절로 한 번에 배치 조회 후 Map으로 매핑

## 코드 예시

```java
// 🔴 Bad: N+1 - 매 반복마다 쿼리 실행
List<SpaceResponse> responses = spaces.stream()
    .map(space -> {
        Long count = guestBookRepository.countBySpace(space);  // N번 쿼리
        SpacePhoto photo = spacePhotoRepository.findBySpace(space);  // N번 쿼리
        return SpaceResponse.from(space, photo, count);
    })
    .toList();

// 🟢 Good: 배치 쿼리로 2번의 쿼리로 해결 (Forgather 실제 예시)
private List<SpaceResponse> createSpaceResponses(List<SpaceHostMap> spaceHostMaps) {
    // 1. ID 목록 추출
    List<Long> spaceIds = spaceHostMaps.stream()
        .map(shm -> shm.getSpace().getId())
        .toList();

    // 2. 배치 쿼리로 한 번에 조회 후 Map 변환
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

    // 3. Map에서 O(1)으로 조회
    return spaceHostMaps.stream()
        .map(shm -> {
            Space space = shm.getSpace();
            Long count = guestBookCardCounts.getOrDefault(space.getId(), 0L);
            SpacePhoto photo = spacePhotos.getOrDefault(space.getId(), SpacePhoto.empty(space));
            return SpaceResponse.from(space, photo, count);
        })
        .toList();
}

// Repository 배치 쿼리 메서드
List<SpacePhoto> findAllBySpaceIdInAndDeletedAtIsNull(List<Long> spaceIds);

@Query("""
    SELECT new com.forgather.domain.guestbook.dto.SpaceGuestBookCountDto(
        g.space.id,
        COUNT(g.id)
    )
    FROM GuestBookCard g
    WHERE g.space.id IN :spaceIds
    AND g.deletedAt IS NULL
    GROUP BY g.space.id
    """)
List<SpaceGuestBookCountDto> countBySpaceIdAndDeletedAtIsNullIn(@Param("spaceIds") List<Long> spaceIds);
```

## 배치 삭제 / 업데이트

```java
// 🔴 Bad: 개별 삭제
for (Card card : cards) {
    cardRepository.delete(card);  // N번 DELETE 쿼리
}

// 🟢 Good: 배치 삭제
@Modifying
@Query("UPDATE GuestBookCard g SET g.deletedAt = CURRENT_TIMESTAMP WHERE g.id IN :ids")
void softDeleteByIds(@Param("ids") List<Long> ids);

// 🔴 Bad: 개별 업데이트
for (Product product : products) {
    product.updateStock(0);
    productRepository.save(product);  // N번 UPDATE 쿼리
}

// 🟢 Good: 배치 업데이트
@Modifying
@Query("UPDATE Product p SET p.stock = :stock WHERE p.id IN :ids")
void updateStockByIds(@Param("ids") List<Long> ids, @Param("stock") int stock);
```

## Batch Size 설정으로 연관 관계 배치 로딩

```yaml
# application.yml
spring.jpa.properties.hibernate.default_batch_fetch_size: 100
```

```java
// 또는 엔티티에 직접 설정
@Entity
public class Order {
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;
}
```

## IN 절 크기 제한 처리

```java
// Oracle: 1000개, 다른 DB: 제한 없거나 높음
// 안전하게 청크 단위로 처리
public List<Product> findByIdsInChunks(List<Long> ids, int chunkSize) {
    List<Product> result = new ArrayList<>();

    for (int i = 0; i < ids.size(); i += chunkSize) {
        List<Long> chunk = ids.subList(i, Math.min(i + chunkSize, ids.size()));
        result.addAll(productRepository.findAllByIdIn(chunk));
    }

    return result;
}
```

## 주의사항

1. **IN 절 크기 제한** — Oracle 1000개, MySQL/PostgreSQL 실질적 제한 없음
2. **결과 없는 ID 처리** — `getOrDefault()` 또는 기본값 처리 필수
3. **IN 절 조회 결과 순서 보장 안 됨** — 순서 중요하면 별도 정렬
4. **대량 데이터 조회 시 메모리 주의** — 페이징 또는 스트림 처리 고려
5. **Soft Delete 조건 주의** — IN 배치 쿼리에도 `deletedAt IS NULL` 포함 필수

## 언제 다른 패턴을 쓸까

- 부모-자식 단일 관계 로딩 → [Fetch Join](./01-fetch-join.md)이 더 단순
- 상황별 페치 전략 → [EntityGraph](./02-entity-graph.md)
- 엔티티 일부 필드만 필요 → [DTO 프로젝션](./03-dto-projection.md)과 결합 가능
