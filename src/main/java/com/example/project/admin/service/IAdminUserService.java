package com.example.project.admin.service;

import com.example.project.admin.dto.AdminUserResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IAdminUserService {
    List<AdminUserResponse> getAllUsers();

    ResponseEntity<?> blockUser(Long id);
}
