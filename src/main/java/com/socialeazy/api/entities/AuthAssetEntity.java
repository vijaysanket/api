package com.socialeazy.api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Entity
@Table(name="AuthAsset")
@NoArgsConstructor
public class AuthAssetEntity {
    @Id
    @Column
    private String state;
    
    @Column
    private String codeVerifier;

    @Column
    private String codeChannelge;

}
