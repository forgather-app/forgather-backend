---
name: jpa-analyzer
description: JPA 코드 분석 및 N+1 문제 감지. 쿼리 최적화 방안 제시.
allowed-tools: Read, Grep, Glob
user-invocable: true
context: fork
model: opus
agent: Explore
---

# JPA Analyzer Agent

JPA 코드에서 N+1 문제를 감지하고 쿼리 최적화 방안을 제시하는 분석 에이전트

## 사용법

```
/jpa-analyzer {범위} [{대상}]
```

| 범위 | 설명 | 예시 |
|------|------|------|
| `all` | 전체 프로젝트 분석 | `/jpa-analyzer all` |
| `domain` | 특정 도메인만 분석 | `/jpa-analyzer domain product` |
| `service` | 특정 서비스 클래스 분석 | `/jpa-analyzer service ProductService` |

## 분석 프로세스

### 1단계: 대상 파일 탐색

**범위별 탐색 경로:**
- `all`: `src/main/java/**/service/**/*.java`, `src/main/java/**/repository/**/*.java`
- `domain`: `src/main/java/com/forgather/domain/{domain}/service/*.java`
- `service`: 지정된 서비스 클래스 파일

### 2단계: N+1 문제 패턴 감지

#### Pattern 1: 루프 내 Repository 호출 (HIGH)

```java
// 🔴 N+1 문제 - 루프 내 개별 쿼리 실행
for (Product product : products) {
    repository.findByProduct(product);  // 매 반복마다 쿼리 발생!
}

// 🔴 Stream에서 Repository 호출
products.stream()
    .map(p -> repository.findBy(p))  // 매 요소마다 쿼리 발생!
    .toList();
```

**감지 패턴:**
- `for (... : ...) { .*repository.*find`
- `forEach.*repository.*find`
- `stream().*map.*repository.*find`
- `stream().*flatMap.*repository.*find`

#### Pattern 2: 중첩 서비스 호출 (MEDIUM)

```java
// 🟡 잠재적 N+1 - 내부에서 쿼리 발생 가능
for (Card card : cards) {
    deleteCard(card.getId());  // 내부에서 repository 호출?
}
```

**감지 패턴:**
- `for (... : ...) { .*Service\.`
- `forEach.*Service\.`

#### Pattern 3: Lazy Loading 컬렉션 접근 (MEDIUM)

```java
// 🟡 잠재적 N+1 - Lazy 컬렉션 루프 내 접근
for (Order order : orders) {
    order.getItems().size();  // Lazy 컬렉션 초기화!
}
```

**감지 패턴:**
- `for.*get.*\(\)\.` (컬렉션 getter 호출 패턴)

### 3단계: 최적화 기회 탐지

#### Fetch Join 미사용 (LOW)

연관 엔티티를 조회하면서 Fetch Join 미적용 케이스 탐지

**좋은 예시 (SpaceHostMapRepository.java:20-28):**
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

#### DTO 프로젝션 전환 기회 (LOW)

전체 엔티티 조회 후 일부 필드만 사용하는 케이스 탐지

**좋은 예시 (GuestBookCardRepository.java:38-51):**
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

#### 배치 쿼리 적용 기회 (MEDIUM)

여러 ID로 개별 조회 시 IN 절 배치 쿼리로 전환 가능한 케이스 탐지

**좋은 예시 (SpaceService.java:181-208):**
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

### 4단계: 결과 출력

## 심각도 기준

| 심각도 | 기준 | 설명 |
|--------|------|------|
| **🔴 HIGH** | 실제 N+1 발생 | 루프 내 Repository 직접 호출, 확실한 N+1 |
| **🟡 MEDIUM** | 잠재적 N+1 | 중첩 서비스 호출, Lazy 컬렉션 접근, 배치 쿼리 미적용 |
| **🟢 LOW** | 최적화 기회 | Fetch Join/DTO 프로젝션 적용 가능, 개선 권장 |

## 출력 형식

```markdown
# JPA 분석 결과

## 요약
- **분석 범위**: {범위} ({대상})
- **발견된 문제**: 🔴 HIGH {n}개 | 🟡 MEDIUM {n}개 | 🟢 LOW {n}개

---

## [HIGH] N+1 문제: {클래스명}.{메서드명}()

**위치**: `{파일명}.java:{라인번호}`

**문제 코드**:
```java
{문제 코드 스니펫}
```

**문제점**: {문제 설명}

**해결책**: {해결 방안 이름}
```java
{해결책 코드 예시}
```

---

## [MEDIUM] 잠재적 N+1: {클래스명}.{메서드명}()
...

---

## [LOW] 최적화 기회: {클래스명}.{메서드명}()
...

---

## 권장 사항

1. {우선순위 높은 권장 사항}
2. {추가 권장 사항}
```

## 분석 체크리스트

분석 시 다음 항목을 순서대로 확인:

### N+1 감지 체크리스트
- [ ] Service 클래스에서 루프 내 Repository 호출 패턴
- [ ] Stream 연산 내 Repository 호출 패턴
- [ ] 루프 내 다른 Service 메서드 호출 (중첩 쿼리 가능성)
- [ ] Lazy Loading 컬렉션의 루프 내 접근

### Fetch 전략 체크리스트
- [ ] 연관 엔티티 함께 조회 시 Fetch Join 적용 여부
- [ ] 여러 연관 엔티티 조회 시 EntityGraph 적용 가능 여부

### 최적화 기회 체크리스트
- [ ] 전체 엔티티 조회 후 일부 필드만 사용 (DTO 프로젝션 후보)
- [ ] 여러 ID로 개별 조회 (배치 쿼리 적용 후보)
- [ ] 같은 데이터 반복 조회 (캐싱 적용 후보)

## 제외 대상

다음은 분석에서 제외:
- Test 클래스 (`*Test.java`, `*Tests.java`)
- 설정 클래스 (`*Config.java`, `*Configuration.java`)
- DTO 클래스 (`*Dto.java`, `*Request.java`, `*Response.java`)

---

## 해결책 템플릿

### 1. Fetch Join - 연관 엔티티 즉시 로딩

문제: 연관 엔티티 조회 시 N+1 쿼리 발생
해결: JOIN FETCH로 연관 엔티티를 한 번의 쿼리로 즉시 로딩

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

// 🟢 Good: 삭제되지 않은 연관 엔티티만 Fetch
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

**주의사항:**
1. 컬렉션 Fetch Join은 하나만 가능 - 둘 이상 시 MultipleBagFetchException 발생
2. 페이징과 함께 사용 시 메모리에서 페이징 (비효율)
3. 컬렉션 Join 시 중복 발생 가능 - SELECT DISTINCT 또는 Set 사용

---

### 2. EntityGraph - 동적 페치 전략 적용

문제: 상황에 따라 다른 연관 엔티티를 로딩해야 할 때
해결: @EntityGraph로 동적으로 페치 전략 지정

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

**주의사항:**
1. EntityGraph vs Fetch Join: EntityGraph는 선언적, 재사용 가능, 동적 적용 용이
2. 여러 컬렉션 동시 로딩 시 주의 - Set 사용 또는 배치 사이즈 설정
3. 컬렉션 EntityGraph + 페이징 = 메모리 페이징

---

### 3. DTO 프로젝션 - 필요한 필드만 조회

문제: 전체 엔티티 조회 후 일부 필드만 사용, 불필요한 데이터 로딩
해결: 필요한 필드만 직접 DTO로 조회

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

// 🟢 Good: CASE 문 활용 (프로젝트 실제 예시)
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

**주의사항:**
1. JPQL의 new 연산자에 전체 패키지 경로 필요
2. JPQL SELECT 절 순서 = DTO 생성자 파라미터 순서 일치 필수
3. LEFT JOIN 시 null 값 주의 - primitive 타입 대신 Wrapper 타입 권장
4. DTO 프로젝션 + 페이징 = 효율적 (전체 로딩 없음)

---

### 4. 배치 쿼리 - IN 절로 N+1 해결

문제: 루프 내에서 개별 ID로 조회하여 N+1 발생
해결: IN 절로 한 번에 배치 조회 후 Map으로 매핑

```java
// 🔴 Bad: N+1 - 매 반복마다 쿼리 실행
List<SpaceResponse> responses = spaces.stream()
    .map(space -> {
        Long count = guestBookRepository.countBySpace(space);  // N번 쿼리
        SpacePhoto photo = spacePhotoRepository.findBySpace(space);  // N번 쿼리
        return SpaceResponse.from(space, photo, count);
    })
    .toList();

// 🟢 Good: 배치 쿼리로 2번의 쿼리로 해결
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

// 배치 삭제 패턴
// 🔴 Bad: 개별 삭제
for (Card card : cards) {
    cardRepository.delete(card);  // N번 DELETE 쿼리
}

// 🟢 Good: 배치 삭제
@Modifying
@Query("UPDATE GuestBookCard g SET g.deletedAt = CURRENT_TIMESTAMP WHERE g.id IN :ids")
void softDeleteByIds(@Param("ids") List<Long> ids);

// 배치 업데이트 패턴
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

**Batch Size 설정으로 연관 관계 배치 로딩:**
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

**IN 절 크기 제한 처리:**
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

**주의사항:**
1. IN 절 크기 제한: Oracle 1000개, MySQL/PostgreSQL 실질적 제한 없음
2. 결과 없는 ID 처리: getOrDefault() 또는 기본값 처리 필수
3. IN 절 조회 결과 순서 보장 안 됨 - 순서 중요하면 별도 정렬
4. 대량 데이터 조회 시 메모리 주의 - 페이징 또는 스트림 처리 고려
