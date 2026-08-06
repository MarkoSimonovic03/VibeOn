package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePostDto {
    @NotBlank
    @Size(max = 255)
    private String newDescription;
}
