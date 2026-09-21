package com.forgather.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.host.model.Host;
import com.forgather.global.util.RandomCodeGenerator;

public class HostFixture {

    private static final String EMAIL = "posty@forgather.app";
    private static final RandomCodeGenerator CODE_GENERATOR = new RandomCodeGenerator();

    /**
     * 운영과 동일한 방식으로 공개 코드를 생성한다. 코드 값 자체를 검증하는 테스트는
     * createHostWithCode로 원하는 값을 직접 지정한다.
     */
    public static String randomCode() {
        return CODE_GENERATOR.generate(Host.CODE_LENGTH);
    }

    public static Host createHost() {
        Host host = new Host(randomCode(), "포스티", EMAIL);
        host.updateNickname("포스티");
        return host;
    }

    public static Host createHostWithName(String name) {
        Host host = new Host(randomCode(), name, EMAIL);
        host.updateNickname(name);
        return host;
    }

    public static Host createHostWithCode(String code) {
        Host host = new Host(code, "포스티", EMAIL);
        host.updateNickname("포스티");
        return host;
    }

    /**
     * 이메일 저장 이전에 가입해 email이 비어 있는 기존 회원을 재현한다.
     * 생성자는 email을 필수로 받으므로, DB에서 로드된 상태를 리플렉션으로 만든다.
     */
    public static Host createHostWithoutEmail(String name) {
        Host host = new Host(randomCode(), name, EMAIL);
        ReflectionTestUtils.setField(host, "email", null);
        return host;
    }

    /**
     * picture_url 컬럼에 값이 남아 있는 기존 회원을 재현한다.
     * 프로필 사진은 HostProfilePhoto 엔티티로 이관되어 이 컬럼에는 더 이상 값을 쓰지 않으므로,
     * DB에서 로드된 상태를 리플렉션으로 만든다.
     */
    public static Host createHostWithLegacyPictureUrl(String pictureUrl) {
        Host host = createHost();
        ReflectionTestUtils.setField(host, "pictureUrl", pictureUrl);
        return host;
    }

    public static Host createHostWithId(long id) {
        Host host = new Host(randomCode(), "포스티", EMAIL);
        ReflectionTestUtils.setField(host, "id", id);
        return host;
    }
}
