package com.forgather.global.auth.model;

import java.util.Objects;

import com.forgather.domain.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Host extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "email")
    private String email;

    @Column(name = "agreed_terms")
    private Boolean agreedTerms = false;

    public Host(String name, String pictureUrl) {
        this(name, pictureUrl, null);
    }

    public Host(String name, String pictureUrl, String email) {
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.email = email;
    }

    public void agreeTerms() {
        this.agreedTerms = true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Host))
            return false;
        Host host = (Host)o;
        return id != null && Objects.equals(id, host.id);
    }

    @Override
    public int hashCode() {
        return Host.class.hashCode();
    }
}
