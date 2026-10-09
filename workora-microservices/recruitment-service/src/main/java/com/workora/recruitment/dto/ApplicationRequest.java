package com.workora.recruitment.dto;

import java.util.List;

public record ApplicationRequest(
        String coverNote,
        List<String> structuredData
) {
}
