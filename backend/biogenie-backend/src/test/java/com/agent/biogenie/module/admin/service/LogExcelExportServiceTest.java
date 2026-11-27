package com.agent.biogenie.module.admin.service;

import com.agent.biogenie.module.admin.entity.AdminAuditLog;
import com.agent.biogenie.module.admin.entity.ApiBehaviorLog;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogExcelExportServiceTest {
    private final LogExcelExportService service = new LogExcelExportService();

    @Test
    void auditExportShouldCreateReadableXlsx() throws Exception {
        AdminAuditLog row = new AdminAuditLog();
        row.setId(1L); row.setOperatorId(7L); row.setAction("UPDATE_ROLE");
        row.setTargetType("USER"); row.setTargetId("9"); row.setCreateTime(LocalDateTime.of(2026, 8, 31, 10, 0));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.exportAudit(List.of(row), response);

        assertTrue(response.getContentType().contains("spreadsheetml"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals("管理员审计日志", workbook.getSheetAt(0).getSheetName());
            assertEquals("操作人ID", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("UPDATE_ROLE", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test
    void behaviorExportShouldCreateReadableXlsx() throws Exception {
        ApiBehaviorLog row = new ApiBehaviorLog();
        row.setId(1L); row.setUserId(8L); row.setUserRole("USER"); row.setRequestMethod("GET");
        row.setRequestPath("/api/user/profile"); row.setStatusCode(200); row.setDurationMs(12L);
        row.setCreateTime(LocalDateTime.of(2026, 8, 31, 10, 0));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.exportBehavior(List.of(row), response);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals("API行为日志", workbook.getSheetAt(0).getSheetName());
            assertEquals("/api/user/profile", workbook.getSheetAt(0).getRow(1).getCell(4).getStringCellValue());
            assertEquals(12D, workbook.getSheetAt(0).getRow(1).getCell(6).getNumericCellValue());
        }
    }
}
