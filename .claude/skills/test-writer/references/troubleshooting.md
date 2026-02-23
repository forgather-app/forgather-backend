# Test Writer Troubleshooting

## TestContainers 연결 실패

**Cause:** Docker가 실행되지 않음
**Solution:** Docker Desktop 실행 확인

```bash
docker ps  # Docker 데몬 실행 여부 확인
```

## RestAssuredMockMvc NPE

**Cause:** `@BeforeEach`에서 mockMvc 설정 누락
**Solution:** `RestAssuredMockMvc.mockMvc(mockMvc)` 호출 확인

```java
@Autowired
private MockMvc mockMvc;

@BeforeEach
void setUp() {
    RestAssuredMockMvc.mockMvc(mockMvc);
}
```

## @Sql 정리 스크립트 실패

**Cause:** FK 제약조건으로 DELETE 순서 문제
**Solution:** 자식 테이블부터 삭제하도록 순서 조정

```sql
-- 자식 테이블 먼저
DELETE FROM product;
DELETE FROM guest_book_card;
-- 부모 테이블 나중에
DELETE FROM space;
DELETE FROM host;
```

## @Transactional 롤백 안 됨

**Cause:** 서비스 테스트에서 `@Transactional` 누락
**Solution:** 서비스 통합 테스트 클래스에 `@Transactional` 추가

```java
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional  // 필수!
class SpaceServiceTest extends TestOnContainer {
```

## Fixture 클래스를 찾을 수 없음

**Cause:** fixture 패키지 경로 오류
**Solution:** `src/test/java/com/forgather/fixture/` 경로 확인

```java
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.HostFixture;
```
