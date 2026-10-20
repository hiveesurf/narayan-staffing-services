package com.narayan.staffing.security;

import com.narayan.staffing.identity.User;
import com.narayan.staffing.identity.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public StaffUserPrincipal loadByEmail(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    public StaffUserPrincipal toPrincipal(User user) {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            role.getPermissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getCode())));
        });
        return new StaffUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getFullName(), user.isActive(), authorities);
    }
}
