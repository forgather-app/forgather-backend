package com.forgather.global.client;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AppVersion(
    int major,
    int minor,
    int patch
) implements Comparable<AppVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static final Comparator<AppVersion> COMPARATOR = Comparator.comparingInt(AppVersion::major)
        .thenComparingInt(AppVersion::minor)
        .thenComparingInt(AppVersion::patch);

    public static Optional<AppVersion> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = VERSION_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new AppVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
            ));
        } catch (NumberFormatException e) {
            // 정규식은 통과했지만 int 범위를 넘는 숫자
            return Optional.empty();
        }
    }

    public boolean isLowerThan(AppVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(AppVersion other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return "%d.%d.%d".formatted(major, minor, patch);
    }
}
