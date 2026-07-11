package com.carebridge.backend.partner.dto.request;
import java.time.LocalDate;
public record PerformanceFilter(LocalDate from,LocalDate to){public boolean isValid(){return from==null||to==null||!from.isAfter(to);}}
