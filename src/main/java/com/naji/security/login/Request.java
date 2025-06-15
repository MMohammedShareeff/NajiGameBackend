package com.naji.security.login;

import com.naji.validation.OnCreate;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Request {

    @NotBlank(message = "please provide a name for your account.", groups = OnCreate.class)
    String userName;
    @NotBlank(message = "please provide a password for your account.",groups = OnCreate.class)
    String password;
}
