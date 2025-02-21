package com.socialeazy.api.controller;

import com.socialeazy.api.services.ChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("v1")
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @GetMapping("/get-auth-url")
    public String getAuthUrl(@RequestParam @Validated String channelName) {
        return channelService.getAuthUrl(channelName);
        //return recordService.createObject(createObjectRequest);
    }

    @PostMapping("/handle-auth-redirection")
    public void handleAuthRedirection(@RequestBody Map<String, String> requestBody) {
        for(Map.Entry entry : requestBody.entrySet()) {
            System.out.println(entry.getKey()+"    "+entry.getValue());
        }

        channelService.handleAuthRedirection(requestBody);


    }
}
