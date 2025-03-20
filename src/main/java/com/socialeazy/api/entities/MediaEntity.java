package com.socialeazy.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Entity
@Table(name="media")
@NoArgsConstructor
public class MediaEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private int pa_id;

    @Column
    private String media_url;

    @Column
    private String media_type;

    @Column
    private LocalDateTime created_at;


}
