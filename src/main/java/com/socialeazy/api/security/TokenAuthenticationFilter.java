package com.socialeazy.api.security;

import com.socialeazy.api.domains.responses.ValidateTokenResponse;
import com.socialeazy.api.repo.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.*;

@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TokenProvider tokenProvider;

    @Override
    public void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        try {
            String userToken = getUserTokenFromRequest(servletRequest);
            if (userToken != null && StringUtils.hasText(userToken)) {
                ValidateTokenResponse validateTokenResponse = tokenProvider.validateToken(userToken);
                int userId = validateTokenResponse.getUserId();
                int orgId = validateTokenResponse.getOrgId();

                // Wrap the request to add headers
                HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(servletRequest) {
                    @Override
                    public String getHeader(String name) {
                        if ("userId".equals(name)) {
                            return String.valueOf(userId);
                        } else if ("orgId".equals(name)) {
                            return String.valueOf(orgId);
                        }
                        return super.getHeader(name);
                    }
                };

                // Set authentication
                User user = new User(String.valueOf(userId), "", new ArrayList<>());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.info("Added userId {} and orgId {} to request headers", userId, orgId);



                CustomHeaderRequestWrapper customHeaderRequestWrapper = new CustomHeaderRequestWrapper(servletRequest, validateTokenResponse);
                Enumeration<String> headerNames = wrappedRequest.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    //log.info("Header {} = {}", headerName, wrappedRequest.getHeader(headerName));
                }

                headerNames = customHeaderRequestWrapper.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    //log.info("Header 1 {} = {}", headerName, wrappedRequest.getHeader(headerName));
                }



                filterChain.doFilter(customHeaderRequestWrapper, servletResponse);

                return;
            }
        } catch (Exception ex) {
            log.error("Error in authentication: " + ex.getMessage());
            servletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String getUserTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return token.equals("null") ? null : token;
        }
        return null;
    }

    // Custom request wrapper to add headers dynamically
//    private static class CustomHeaderRequestWrapper extends HttpServletRequestWrapper {
//        private final Map<String, String> customHeaders = new HashMap<>();
//
//        public CustomHeaderRequestWrapper(HttpServletRequest request, ValidateTokenResponse validateTokenResponse) {
//            super(request);
//            customHeaders.put("userId", String.valueOf(validateTokenResponse.getUserId()));
//            customHeaders.put("orgId", String.valueOf(validateTokenResponse.getOrgId()));
//        }
//
//        @Override
//        public String getHeader(String name) {
//            return customHeaders.getOrDefault(name, super.getHeader(name));
//        }
//
//        @Override
//        public Enumeration<String> getHeaderNames() {
//            List<String> names = Collections.list(super.getHeaderNames());
//            names.addAll(customHeaders.keySet());
//            return Collections.enumeration(names);
//        }
//
//
//        @Override
//        public int getIntHeader(String name) {
//            return customHeaders.containsKey(name) ? Integer.parseInt(customHeaders.get(name)) : super.getIntHeader(name);
//        }
//    }
}
