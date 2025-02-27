package com.socialeazy.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="Accounts")
@NoArgsConstructor
public class AccountsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private String accountHandle;

    @Column
    private String accessToken;

    @Column
    private String refreshToken;

    @Column
    private LocalDateTime validTill;

    @Column
    private LocalDateTime connectedAt;

    @Column
    private int followerCount;

    @Column
    private String accountOf;

    @Column
    private int userId;

    @Column
    private String profilePicture;

    @Column
    private String accountName;

    @Column
    private String channelId;

}
