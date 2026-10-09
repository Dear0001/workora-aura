package com.workora.organization.dto;

import com.workora.organization.entity.CompanyStatus;
import jakarta.validation.constraints.NotNull;

public record AdminCompanyStatusRequest(@NotNull CompanyStatus status) {
}
