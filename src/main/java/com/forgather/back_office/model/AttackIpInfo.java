package com.forgather.back_office.model;

import java.util.Arrays;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

import lombok.Getter;

@Getter
public class AttackIpInfo {

    private static final int IPV4_OCTET_COUNT = 4;
    private static final int OCTET_MIN = 0;
    private static final int OCTET_MAX = 255;

    private final String ip;
    private final long count;

    public AttackIpInfo(String ip, long count) {
        validateRequiredFields(ip, count);
        validateIp(ip);
        this.ip = ip;
        this.count = count;
    }

    private void validateRequiredFields(String ip, long count) {
        if (ip == null) {
            throw new BaseNullPointerException("IP 주소는 null일 수 없습니다.");
        }
        if (count < 0) {
            throw new BaseException("공격 횟수는 음수일 수 없습니다.");
        }
    }

    private void validateIp(String ip) {
        String[] octets = ip.split("\\.", -1);
        if (octets.length != IPV4_OCTET_COUNT) {
            throw new BaseException("유효하지 않은 IP 주소 형식입니다: " + ip);
        }
        Arrays.stream(octets)
            .forEach(octet -> validateOctet(ip, octet));
    }

    private void validateOctet(String ip, String octet) {
        try {
            int value = Integer.parseInt(octet);
            if (value < OCTET_MIN || value > OCTET_MAX) {
                throw new BaseException("유효하지 않은 IP 주소 형식입니다: " + ip);
            }
        } catch (NumberFormatException e) {
            throw new BaseException("유효하지 않은 IP 주소 형식입니다: " + ip);
        }
    }


}
