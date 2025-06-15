package com.naji.security.jwt;

import com.naji.player.Player;
import com.naji.player.PlayerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
public class AuthenticationTokenFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;
    private final PlayerRepository playerRepository;

    private final UserDetailsService userDetailsService;

    public AuthenticationTokenFilter(
            JWTUtils jwtUtils, PlayerRepository playerRepository,
            @Lazy UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.playerRepository = playerRepository;
        this.userDetailsService = userDetailsService;
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (requestURI.equals("/player/reset-password") || requestURI.equals("/player/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.debug("AuthTokenFilter called for URI: {}", request.getRequestURI());
        try {
            String authHeader = request.getHeader("Authorization");
            String token = parseJwt(authHeader);
            if (Objects.nonNull(token) && jwtUtils.isAuthorizedToken(token)) {
                String username = jwtUtils.getUserNameFromJwtToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                Player player = playerRepository.findByUserName(username)
                        .orElseThrow(() -> new UsernameNotFoundException("user name not found"));
                String role = player.getRole();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, username, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        logger.debug("AuthTokenFilter.java: {}", token);
        return token;
    }
}
