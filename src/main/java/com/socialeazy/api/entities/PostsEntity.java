package com.socialeazy.api.entities;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="Post")
@NoArgsConstructor
public class PostsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private int userId;

    @Column
    private LocalDateTime addedAt;

    @Column
    private LocalDateTime scheduledAt;

    @Column
    private String status;

    @Column
    private int orgId;

//    @Column
//    private int channelId;

}
