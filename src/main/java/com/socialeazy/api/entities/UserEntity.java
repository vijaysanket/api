package com.socialeazy.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name="User")
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private int orgId;

    @Column
    private String name;

    @Column
    private LocalDateTime addedAt;

    @Column
    private boolean isActive = true;

    @Column
    private String emailId;

    @Column
    private String password;

}
