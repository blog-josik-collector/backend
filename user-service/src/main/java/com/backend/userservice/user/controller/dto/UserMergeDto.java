package com.backend.userservice.user.controller.dto;

import java.util.UUID;

public record UserMergeDto() {

    public record Request(UUID userId) {

    }
}
