package com.naji.player;

import com.naji.validation.OnCreate;
import com.naji.validation.OnUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(
            message = "please provide an email",
            groups = OnCreate.class
    )
    @Email(
            message = "Invalid email format",
            groups = {OnUpdate.class, OnCreate.class}
    )
    private final String email;

    @NotBlank(
            message = "please provide a password for your account",
            groups = OnCreate.class
    )
    private String newPassword;

    @NotBlank(
            message = "please provide a password for your account",
            groups = OnCreate.class
    )
    private String newPasswordAgain;
}
