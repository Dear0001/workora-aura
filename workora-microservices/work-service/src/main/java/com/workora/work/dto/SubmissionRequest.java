package com.workora.work.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmissionRequest(
        @Size(max = 4000) String note,
        List<String> attachmentKeys
) {
}
