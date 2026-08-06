package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {
    @NotBlank
    @Size(max = 255)
    private String username;

    @NotBlank
    @Size(max = 255)
    private String password;
}
