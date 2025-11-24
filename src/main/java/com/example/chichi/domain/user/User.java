package com.example.chichi.domain.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "users")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private long id;

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
        this.discordId = discordId;
        this.pin = pin;
        this.roleTypes = roleTypes;
    }

    public void updatePin(String pin) {
        this.pin = pin;
    }
    public void addRole(RoleType roleType) {
        roleTypes.add(roleType);
    }
    public void removeRole(RoleType roleType){
        roleTypes.remove(roleType);
    }
}
