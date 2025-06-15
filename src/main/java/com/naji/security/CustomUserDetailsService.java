package com.naji.security;

import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.player.Player;
import com.naji.player.PlayerServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;


@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private PlayerServiceImpl playerServiceImpl;

    @Override
    public UserDetails loadUserByUsername(String userName) throws ResourceNotFoundException {
        Player player = playerServiceImpl.getPlayerByUserNameOrThrowException(userName);

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + player.getRole());
        return new org.springframework.security.core.userdetails.User(
                player.getUserName(),
                player.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
