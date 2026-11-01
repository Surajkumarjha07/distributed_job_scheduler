package com.distributed.jobscheduler.dto;

import com.distributed.jobscheduler.enums.NotificationChannel;
import com.distributed.jobscheduler.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrationPayload(
        @NotBlank String from,
        @Email String to,
        @NotBlank String username,
        @NotNull NotificationType type,
        @NotNull NotificationChannel channel
) implements NotificationSource {
    @Override public String sender() { return from; }
    @Override public String recipient() { return to; }
}
