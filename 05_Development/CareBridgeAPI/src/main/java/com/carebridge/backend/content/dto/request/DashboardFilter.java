package com.carebridge.backend.content.dto.request;

import java.time.LocalDate;
import org.springframework.lang.Nullable;

public record DashboardFilter(@Nullable LocalDate from, @Nullable LocalDate to) {
}
