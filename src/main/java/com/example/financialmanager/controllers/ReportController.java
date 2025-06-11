package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.MonthlyReportDto;
import com.example.financialmanager.dtos.YearlyReportDto;
import com.example.financialmanager.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
// Import for validation annotations if used on path variables
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated; // Required for class-level validation activation for path variables

@RestController
@RequestMapping("/api/reports")
@Validated // To enable validation of path variables
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportDto> getMonthlyReport(
            @PathVariable int year, // Basic year validation can be added if necessary (e.g. @Min(1900))
            @PathVariable @Min(1) @Max(12) int month) {
        String userEmail = getCurrentUserEmail();
        MonthlyReportDto report = reportService.getMonthlyReport(year, month, userEmail);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/yearly/{year}")
    public ResponseEntity<YearlyReportDto> getYearlyReport(
            @PathVariable int year) { // Basic year validation can be added
        String userEmail = getCurrentUserEmail();
        YearlyReportDto report = reportService.getYearlyReport(year, userEmail);
        return ResponseEntity.ok(report);
    }
}
