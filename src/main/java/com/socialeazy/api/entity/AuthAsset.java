package com.socialeazy.api.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "twitter_auth_asset")
public class AuthAsset {

    @Id
    @Column
    private  String state;

    @Column
    private String codeChallenge;

    @Column
    private String codeVerifier;

    @Column
    private String status;
}


