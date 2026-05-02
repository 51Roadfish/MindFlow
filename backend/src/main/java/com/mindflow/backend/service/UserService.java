package com.mindflow.backend.service;

import com.mindflow.backend.dto.request.LoginRequest;
import com.mindflow.backend.dto.request.RegisterRequest;

public interface UserService {
    void registerUser(RegisterRequest request);
    String loginUser(LoginRequest request);
}
