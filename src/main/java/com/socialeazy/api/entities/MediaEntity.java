package com.socialeazy.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@Data
@Entity
@Table(name="media")
@NoArgsConstructor
public class MediaEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private int contentId;

    @Column(name="media_url")
    private String mediaUrl;

    @Column(name = "media_type")
    private String mediaType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
