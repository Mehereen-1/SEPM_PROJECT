package com.example.project.repository;

import java.util.Optional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.project.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        select distinct u
        from User u
        join u.roles r
        where upper(r.name) in :roleNames
        """)
    List<User> findByAnyRoleNames(@Param("roleNames") Collection<String> roleNames);
}
