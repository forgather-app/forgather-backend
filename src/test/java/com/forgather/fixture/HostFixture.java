package com.forgather.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.global.auth.model.Host;

public class HostFixture {

    private static final String EMAIL = "posty@forgather.app";

    public static Host createHost() {
        Host host = new Host("포스티", EMAIL);
        host.updateNickname("포스티");
        return host;
    }

    public static Host createHostWithName(String name) {
        Host host = new Host(name, EMAIL);
        host.updateNickname(name);
        return host;
    }

    /**
     * 이메일 저장 이전에 가입해 email이 비어 있는 기존 회원을 재현한다.
     * 생성자는 email을 필수로 받으므로, DB에서 로드된 상태를 리플렉션으로 만든다.
     */
    public static Host createHostWithoutEmail(String name) {
        Host host = new Host(name, EMAIL);
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
        Host host = new Host("포스티", EMAIL);
        ReflectionTestUtils.setField(host, "id", id);
        return host;
    }
}
