package com.socialeazy.api.security;

import com.socialeazy.api.domains.responses.ValidateTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class CustomHeaderRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> customHeaders = new HashMap<>();

    public CustomHeaderRequestWrapper(HttpServletRequest request, ValidateTokenResponse validateTokenResponse) {
        super(request);
        customHeaders.put("userId", String.valueOf(validateTokenResponse.getUserId()));
        customHeaders.put("orgId", String.valueOf(validateTokenResponse.getOrgId()));
    }


    public void addHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = customHeaders.get(name);
        return headerValue != null ? headerValue : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> headerNames = new ArrayList<>(customHeaders.keySet());
        Enumeration<String> originalHeaderNames = super.getHeaderNames();
        while (originalHeaderNames.hasMoreElements()) {
            headerNames.add(originalHeaderNames.nextElement());
        }
        return Collections.enumeration(headerNames);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(List.of(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }
}
