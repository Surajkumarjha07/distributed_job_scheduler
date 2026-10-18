package com.distributed.jobscheduler.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record CreateJobDTO(
        @NotBlank
        String userId,

        @NotBlank
        String type,

        @NotEmpty
        Map<String, Object> payload,

        @NotBlank
        String message,

        @NotNull
        @FutureOrPresent
        Instant runAt
) { }
