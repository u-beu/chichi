package com.example.chichi.domain.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity(name = "users")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private long discordId;

    @Column(nullable = false)
    private String pin;

    @Column(name = "user_roles", length = 20)
    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<RoleType> roleTypes = new HashSet<>();

    @CreatedDate
    private LocalDateTime createdDate = LocalDateTime.now();

    @LastModifiedDate
    private LocalDateTime modifiedDate;

    @Builder
    public User(long discordId, String pin, Set<RoleType> roleTypes) {
        if (discordId <= 0) throw new IllegalArgumentException("invalid discordId");
        this.discordId = discordId;

        if (!StringUtils.hasText(pin)) throw new IllegalArgumentException("invalid pin");
        this.pin = pin;

        this.roleTypes = (roleTypes != null && !roleTypes.isEmpty()) ?
                new HashSet<>(roleTypes) : new HashSet<>(Set.of(RoleType.GUEST));
    }

    public void updatePin(String pin) {
        if (!StringUtils.hasText(pin)) throw new IllegalArgumentException("invalid pin");
        this.pin = pin;
    }

    public void addRole(RoleType roleType) {
        Objects.requireNonNull(roleType, "invalid roleType");
        roleTypes.add(roleType);
    }

    public void removeRole(RoleType roleType) {
        Objects.requireNonNull(roleType, "invalid roleType");
        if (roleTypes.size() == 1) throw new IllegalArgumentException("cannot remove one last role");
        roleTypes.remove(roleType);
    }
}
