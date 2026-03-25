package com.example.project.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.project.entity.User;
import com.example.project.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        System.out.println("\n@@@@ LOADING USER DETAILS @@@@");
        System.out.println("Email: " + email);
        System.out.println("User ID: " + user.getId());
        System.out.println("Roles in Database: " + user.getRoles());
        
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> {
                    String authority = "ROLE_" + role.getName();
                    System.out.println("  - Role '" + role.getName() + "' -> Authority '" + authority + "'");
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toSet());
        
        if (authorities.isEmpty()) {
            System.out.println("WARNING: User has no roles! Defaulting to ROLE_BOOK_FRIEND");
            authorities.add(new SimpleGrantedAuthority("ROLE_BOOK_FRIEND"));
        }

        boolean enabled = !Boolean.FALSE.equals(user.getActive());
        
        System.out.println("Final Authorities: " + authorities);
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@");
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
            enabled,
            true,
            true,
            true,
            authorities
        );
    }
}
