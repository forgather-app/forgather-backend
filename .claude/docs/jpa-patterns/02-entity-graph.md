# EntityGraph — 동적 페치 전략 적용

**문제**: 상황에 따라 다른 연관 엔티티를 로딩해야 할 때
**해결**: `@EntityGraph`로 동적으로 페치 전략 지정

## 코드 예시

```java
// 엔티티에 NamedEntityGraph 정의
@Entity
@NamedEntityGraph(
    name = "Order.withItemsAndCustomer",
    attributeNodes = {
        @NamedAttributeNode("items"),
        @NamedAttributeNode("customer")
    }
)
@NamedEntityGraph(
    name = "Order.withItems",
    attributeNodes = @NamedAttributeNode("items")
)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
}

// Repository에서 EntityGraph 사용
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Named EntityGraph 참조
    @EntityGraph(value = "Order.withItemsAndCustomer")
    Optional<Order> findWithItemsAndCustomerById(Long id);

    // 인라인 EntityGraph 정의 (NamedEntityGraph 없이 사용)
    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Order> findWithDetailsById(Long id);

    // 중첩 연관 엔티티 로딩
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    Optional<Order> findWithItemsAndProductsById(Long id);

    // JPQL + EntityGraph 조합
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deletedAt IS NULL")
    List<Order> findByStatusWithItems(@Param("status") OrderStatus status);
}
```

## 주의사항

1. **EntityGraph vs Fetch Join** — EntityGraph는 선언적, 재사용 가능, 동적 적용 용이
2. **여러 컬렉션 동시 로딩 시 주의** — `Set` 사용 또는 배치 사이즈 설정
3. **컬렉션 EntityGraph + 페이징 = 메모리 페이징** (Fetch Join과 동일한 한계)

## 언제 다른 패턴을 쓸까

- 단일 고정 로딩 전략이면 충분할 때 → [Fetch Join](./01-fetch-join.md)이 더 단순
- 엔티티의 일부 필드만 필요 → [DTO 프로젝션](./03-dto-projection.md)
- 부모 리스트와 자식 개별 카운트 → [배치 쿼리](./04-batch-query.md)
