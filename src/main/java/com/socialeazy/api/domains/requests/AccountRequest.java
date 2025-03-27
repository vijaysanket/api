package com.socialeazy.api.domains.requests;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.List;

@Data
public class AccountRequest {


    @NotNull
    private  String channel;

    @NotNull
    private String channelId;

    @Column
    private String contentType;

    @Column
    private String postType;

    @Column
    private List<String> medianames;

    @Column
    private String text;

}
