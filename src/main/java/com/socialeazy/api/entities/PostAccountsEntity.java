package com.socialeazy.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name="PostAccounts")
@NoArgsConstructor
public class PostAccountsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private int postId;

    @Column
    private int accountId;

    @Column
    private String status;
}
