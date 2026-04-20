# Fetch Join — 연관 엔티티 즉시 로딩

**문제**: 연관 엔티티 조회 시 N+1 쿼리 발생
**해결**: JOIN FETCH로 연관 엔티티를 한 번의 쿼리로 즉시 로딩

## 코드 예시

```java
// 🔴 Bad: N+1 발생 - 각 Order의 OrderItems를 개별 쿼리로 조회
@Query("SELECT o FROM Order o WHERE o.status = :status")
List<Order> findByStatus(@Param("status") OrderStatus status);

// 사용 시점에서 Lazy 로딩 발생
orders.forEach(order -> {
    order.getItems().forEach(item -> {  // 매 Order마다 추가 쿼리!
        // ...
    });
});

// 🟢 Good: JOIN FETCH로 한 번에 조회
@Query("""
    SELECT o
    FROM Order o
    JOIN FETCH o.items
    WHERE o.status = :status
    """)
List<Order> findByStatusWithItems(@Param("status") OrderStatus status);

// 🟢 Good: 여러 연관 엔티티 동시 로딩
@Query("""
    SELECT o
    FROM Order o
    JOIN FETCH o.customer c
    JOIN FETCH o.items i
    JOIN FETCH i.product
    WHERE o.id = :orderId
    """)
Optional<Order> findByIdWithCustomerAndItems(@Param("orderId") Long orderId);

// 🟢 Good: 삭제되지 않은 연관 엔티티만 Fetch (Forgather 프로젝트 예시)
@Query("""
    SELECT s
    FROM Space s
    JOIN FETCH s.products p
    WHERE s.code = :code
    AND s.deletedAt IS NULL
    AND p.deletedAt IS NULL
    """)
Optional<Space> findByCodeWithProducts(@Param("code") String code);

// 중복 제거 예시
@Query("""
    SELECT DISTINCT o
    FROM Order o
    JOIN FETCH o.items
    WHERE o.customer.id = :customerId
    """)
List<Order> findByCustomerIdWithItems(@Param("customerId") Long customerId);
```

## 주의사항

1. **컬렉션 Fetch Join은 하나만 가능** — 둘 이상 시 `MultipleBagFetchException` 발생
2. **페이징과 함께 사용 시 메모리에서 페이징** (비효율)
3. **컬렉션 Join 시 중복 발생 가능** — `SELECT DISTINCT` 또는 `Set` 사용

## 언제 다른 패턴을 쓸까

- 같은 엔티티에 상황별로 다른 로딩 전략이 필요 → [EntityGraph](./02-entity-graph.md)
- 페이징이 반드시 필요한 컬렉션 조회 → [DTO 프로젝션](./03-dto-projection.md)
- 부모 리스트에서 각 부모의 자식을 로드하려 하는데 페이징 필요 → [배치 쿼리](./04-batch-query.md)
