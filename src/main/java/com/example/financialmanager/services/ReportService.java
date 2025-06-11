package com.example.financialmanager.services;

import com.example.financialmanager.dtos.MonthlyReportDto;
import com.example.financialmanager.dtos.YearlyReportDto;

public interface ReportService {

    MonthlyReportDto getMonthlyReport(int year, int month, String userEmail);

    YearlyReportDto getYearlyReport(int year, String userEmail);
}
