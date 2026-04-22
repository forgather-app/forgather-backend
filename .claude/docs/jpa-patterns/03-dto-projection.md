# DTO 프로젝션 — 필요한 필드만 조회

**문제**: 전체 엔티티 조회 후 일부 필드만 사용, 불필요한 데이터 로딩
**해결**: 필요한 필드만 직접 DTO로 조회

## 코드 예시

```java
// 🔴 Bad: 전체 엔티티 조회 후 일부만 사용
List<Order> orders = orderRepository.findAll();
List<OrderSummaryResponse> responses = orders.stream()
    .map(order -> new OrderSummaryResponse(
        order.getId(),          // id만 필요
        order.getOrderNumber(), // orderNumber만 필요
        order.getTotalAmount()  // totalAmount만 필요
        // 나머지 필드는 사용하지 않음
    ))
    .toList();

// 🟢 Good: 생성자 기반 DTO 프로젝션 (JPQL)
public record OrderSummaryDto(
    Long id,
    String orderNumber,
    BigDecimal totalAmount
) {}

@Query("""
    SELECT new com.forgather.domain.order.dto.OrderSummaryDto(
        o.id,
        o.orderNumber,
        o.totalAmount
    )
    FROM Order o
    WHERE o.status = :status
    AND o.deletedAt IS NULL
    """)
List<OrderSummaryDto> findSummaryByStatus(@Param("status") OrderStatus status);

// 🟢 Good: 연관 엔티티 필드 포함 DTO 프로젝션
public record OrderWithCustomerDto(
    Long orderId,
    String orderNumber,
    String customerName,
    String customerEmail
) {}

@Query("""
    SELECT new com.forgather.domain.order.dto.OrderWithCustomerDto(
        o.id,
        o.orderNumber,
        c.name,
        c.email
    )
    FROM Order o
    JOIN o.customer c
    WHERE o.id = :orderId
    AND o.deletedAt IS NULL
    """)
Optional<OrderWithCustomerDto> findWithCustomerById(@Param("orderId") Long orderId);

// 🟢 Good: 집계 함수 포함 DTO 프로젝션
public record OrderStatsDto(
    Long customerId,
    Long orderCount,
    BigDecimal totalAmount
) {}

@Query("""
    SELECT new com.forgather.domain.order.dto.OrderStatsDto(
        o.customer.id,
        COUNT(o.id),
        SUM(o.totalAmount)
    )
    FROM Order o
    WHERE o.customer.id IN :customerIds
    AND o.deletedAt IS NULL
    GROUP BY o.customer.id
    """)
List<OrderStatsDto> findStatsByCustomerIds(@Param("customerIds") List<Long> customerIds);

// 🟢 Good: CASE 문 활용 (Forgather 프로젝트 실제 예시)
public record GuestBookCardListDto(
    Long id,
    String nickname,
    Boolean isRead,
    Boolean hasPhoto
) {}

@Query("""
    SELECT new com.forgather.domain.guestbook.dto.GuestBookCardListDto(
        g.id,
        guest.nickname,
        g.isRead,
        CASE WHEN (
            SELECT COUNT(p) FROM GuestBookCardPhoto p WHERE p.guestBookCard = g
        ) > 0 THEN true ELSE false END
    )
    FROM GuestBookCard g
    JOIN g.guest guest
    WHERE g.space = :space
    AND g.deletedAt IS NULL
    """)
Page<GuestBookCardListDto> findAllDtoBySpace(@Param("space") Space space, Pageable pageable);
```

## 주의사항

1. **JPQL의 `new` 연산자에 전체 패키지 경로 필요**
2. **JPQL SELECT 절 순서 = DTO 생성자 파라미터 순서 일치 필수**
3. **LEFT JOIN 시 null 값 주의** — primitive 타입 대신 Wrapper 타입 권장
4. **DTO 프로젝션 + 페이징 = 효율적** (전체 로딩 없음)

## 언제 다른 패턴을 쓸까

- 엔티티 자체의 상태 변경·도메인 메서드 호출이 필요 → [Fetch Join](./01-fetch-join.md)
- 상황별 페치 전략 전환 → [EntityGraph](./02-entity-graph.md)
- 부모 리스트 + 자식 집계 조합 → [배치 쿼리](./04-batch-query.md)
