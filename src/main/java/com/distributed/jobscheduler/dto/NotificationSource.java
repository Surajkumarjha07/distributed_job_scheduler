package com.distributed.jobscheduler.dto;

import com.distributed.jobscheduler.enums.NotificationChannel;
import com.distributed.jobscheduler.enums.NotificationType;

public interface NotificationSource {
    String sender();
    String recipient();
    NotificationType type();
    NotificationChannel channel();
}