package com.socialeazy.api.controller;

import com.socialeazy.api.domains.responses.ConnectedAccountResponse;
import com.socialeazy.api.services.ChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("v1")
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @GetMapping("/get-auth-url")
    public RedirectView getAuthUrl(@RequestParam @Validated String channelName) {
        String authUrl = channelService.getAuthUrl(channelName);
        return new RedirectView(authUrl);
        //return recordService.createObject(createObjectRequest);
    }

    @PostMapping("/handle-auth-redirection")
    public void handleAuthRedirection(@RequestBody Map<String, String> requestBody) {
        for(Map.Entry entry : requestBody.entrySet()) {
            System.out.println(entry.getKey()+"    "+entry.getValue());
        }
        channelService.handleAuthRedirection(requestBody);
    }

    @GetMapping("/connected-accounts")
    public ConnectedAccountResponse getConnectedAccounts(@RequestHeader int userId, @RequestHeader int orgId) {
        return channelService.getConnectedAccounts(userId, orgId);
    }
}
