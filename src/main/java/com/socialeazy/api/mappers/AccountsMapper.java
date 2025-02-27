package com.socialeazy.api.mappers;

import com.socialeazy.api.domains.responses.ConnectedAccountData;
import com.socialeazy.api.entities.AccountsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountsMapper {
    @Mapping(source = "accountHandle", target = "accountHandle")
    @Mapping(source = "accountOf", target = "channelName")
    ConnectedAccountData mapToResponse(AccountsEntity accountsEntity);

    List<ConnectedAccountData> mapToResponseList(List<AccountsEntity> objects);
}
