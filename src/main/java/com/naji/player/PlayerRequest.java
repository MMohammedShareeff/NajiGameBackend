package com.naji.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.naji.dashboard.Dashboard;
import com.naji.validation.OnCreate;
import com.naji.validation.OnUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class PlayerRequest implements Serializable {

    private long id;

    @NotBlank(
            message = "please provide a name",
            groups = OnCreate.class
    )
    @Size(
            min = 4, message = "Name must be at least 4 characters long",
            groups = {OnCreate.class, OnUpdate.class}
    )
    private String userName;

    @Size(
            min = 8, message = "Password must be at least 8 characters long",
            groups = {OnCreate.class, OnUpdate.class}
    )


    @NotBlank(
            message = "please provide a password for your account",
            groups = OnCreate.class
    )
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\W).{8,}$",
            message = "Password must contain at least one uppercase letter, " +
                    "one lowercase letter, and one special character."
    )
    private String password;

    @NotBlank(
            message = "please provide an email",
            groups = OnCreate.class
    )
    @Email(
            message = "Invalid email format",
            groups = {OnUpdate.class, OnCreate.class}
    )

    private String email;

    private Dashboard dashboard;
    private Instant createdAt;
    private String role;

}
