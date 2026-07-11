package com.forgather.domain.term.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum TermType {
    SERVICE(true),
    PRIVACY(true),
    MARKETING(false);

    private final boolean required;

    TermType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    private static final Set<TermType> REQUIRED_TYPES =
        Arrays.stream(values())
            .filter(TermType::isRequired)
            .collect(Collectors.toUnmodifiableSet());

    public static Set<TermType> requiredTypes() {
        return REQUIRED_TYPES;
    }
}
