package com.forgather.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 어드민 유저 생성 시 암호화된 비밀번호를 얻기 위한 유틸리티.
 * main 메서드를 실행하면 평문 비밀번호가 BCrypt로 암호화되어 출력된다.
 */
public class AdminPasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "password";

        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("평문: " + rawPassword);
        System.out.println("암호화: " + encodedPassword);
    }
}
