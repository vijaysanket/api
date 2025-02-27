package com.socialeazy.api.services.impl;

import com.socialeazy.api.constants.RuntimeConstants;
import com.socialeazy.api.domains.responses.ConnectedAccountData;
import com.socialeazy.api.domains.responses.ConnectedAccountMeta;
import com.socialeazy.api.domains.responses.ConnectedAccountResponse;
import com.socialeazy.api.mappers.AccountsMapper;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.services.ChannelService;
import com.socialeazy.api.services.Connector;
import org.apache.catalina.core.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChannelServiceImpl implements ChannelService {

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private AccountsMapper accountsMapper;

    @Autowired
    private RuntimeConstants runtimeConstants;

    @Override
    public String getAuthUrl(String connectorName) {
        return runtimeConstants.channels.get(connectorName).getAuthUrl();
    }

    @Override
    public void handleAuthRedirection(Map<String, String> requestBody) {
        runtimeConstants.channels.get(requestBody.get("channel")).handleAuthRedirect(requestBody);
    }

    @Override
    public ConnectedAccountResponse getConnectedAccounts(int userId, int orgId) {
        List<ConnectedAccountData> connectedAccountDataList = accountsMapper.mapToResponseList(accountsRepo.findByUserId(userId));
        ConnectedAccountResponse response = new ConnectedAccountResponse();
        response.setData(connectedAccountDataList);
        response.setConnectedChannels(connectedAccountDataList.size());
        return response;
    }


}
