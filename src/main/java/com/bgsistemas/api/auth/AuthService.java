package com.bgsistemas.api.auth;

import com.bgsistemas.api.auth.dtos.LoginResponseDTO;
import com.bgsistemas.api.auth.dtos.RegisterRequestDTO;
import com.bgsistemas.api.auth.dtos.RegisterResponseDTO;
import com.bgsistemas.api.user.User;

public interface AuthService {

    LoginResponseDTO login(User user);

    RegisterResponseDTO register(RegisterRequestDTO request);
}
