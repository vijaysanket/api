package com.socialeazy.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Entity
@Table(name = "content")
@NoArgsConstructor
public class ContentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column
    private int postId;

    @Column(name = "pa_id")
    private int paId;

    private String contentType;

    private String postType;

    @Column
    private String text;



}
