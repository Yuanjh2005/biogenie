package com.agent.biogenie.module.admin.service;

import com.agent.biogenie.module.admin.entity.AdminAuditLog;
import com.agent.biogenie.module.admin.entity.ApiBehaviorLog;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
public class LogExcelExportService {

    public void exportAudit(List<AdminAuditLog> rows, HttpServletResponse response) throws IOException {
        try (SXSSFWorkbook workbook = workbook()) {
            Sheet sheet = workbook.createSheet("管理员审计日志");
            String[] headers = {"ID", "操作人ID", "操作类型", "目标类型", "目标ID", "详情", "IP", "操作时间"};
            prepareSheet(sheet, headers, workbook);
            CellStyle dateStyle = dateStyle(workbook);
            int rowIndex = 1;
            for (AdminAuditLog item : rows) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, string(item.getId())); text(row, 1, string(item.getOperatorId()));
                text(row, 2, item.getAction()); text(row, 3, item.getTargetType());
                text(row, 4, item.getTargetId()); text(row, 5, item.getDetail());
                text(row, 6, item.getIp()); date(row, 7, item.getCreateTime(), dateStyle);
            }
            widths(sheet, 12, 14, 22, 16, 20, 60, 18, 22);
            write(workbook, response, "admin-audit-" + LocalDate.now() + ".xlsx");
        }
    }

    public void exportBehavior(List<ApiBehaviorLog> rows, HttpServletResponse response) throws IOException {
        try (SXSSFWorkbook workbook = workbook()) {
            Sheet sheet = workbook.createSheet("API行为日志");
            String[] headers = {"ID", "用户ID", "调用时角色", "HTTP方法", "请求路径", "状态码", "耗时(ms)", "IP", "User-Agent", "调用时间"};
            prepareSheet(sheet, headers, workbook);
            CellStyle dateStyle = dateStyle(workbook);
            int rowIndex = 1;
            for (ApiBehaviorLog item : rows) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, string(item.getId())); text(row, 1, string(item.getUserId()));
                text(row, 2, item.getUserRole()); text(row, 3, item.getRequestMethod());
                text(row, 4, item.getRequestPath()); number(row, 5, item.getStatusCode());
                number(row, 6, item.getDurationMs()); text(row, 7, item.getIp());
                text(row, 8, item.getUserAgent()); date(row, 9, item.getCreateTime(), dateStyle);
            }
            widths(sheet, 12, 14, 16, 14, 48, 12, 14, 18, 60, 22);
            write(workbook, response, "api-behavior-" + LocalDate.now() + ".xlsx");
        }
    }

    private SXSSFWorkbook workbook() {
        SXSSFWorkbook workbook = new SXSSFWorkbook(500);
        workbook.setCompressTempFiles(true);
        return workbook;
    }

    private void prepareSheet(Sheet sheet, String[] headers, Workbook workbook) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
        Row row = sheet.createRow(0);
        row.setHeightInPoints(24);
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(style);
        }
    }

    private CellStyle dateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

    private void text(Row row, int col, String value) { row.createCell(col).setCellValue(value == null ? "" : value); }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private void number(Row row, int col, Number value) {
        if (value == null) row.createCell(col).setBlank(); else row.createCell(col).setCellValue(value.doubleValue());
    }
    private void date(Row row, int col, java.time.LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    private void widths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, Math.min(widths[i], 100) * 256);
    }
    private void write(SXSSFWorkbook workbook, HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        workbook.write(response.getOutputStream());
        response.flushBuffer();
        workbook.dispose();
    }
}
