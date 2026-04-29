package org.example.service.parser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class ExcelDocumentParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(ExcelDocumentParser.class);

    @Override
    public boolean supports(String fileType) {
        return "xlsx".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws Exception {
        logger.info("解析Excel文档: {}", fileName);

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            StringBuilder fullText = new StringBuilder();
            DataFormatter formatter = new DataFormatter();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                fullText.append("--- 工作表: ").append(sheet.getSheetName()).append(" ---\n\n");

                for (Row row : sheet) {
                    StringBuilder rowText = new StringBuilder();
                    for (Cell cell : row) {
                        String cellValue = formatter.formatCellValue(cell);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            if (rowText.length() > 0) rowText.append(" | ");
                            rowText.append(cellValue.trim());
                        }
                    }
                    if (rowText.length() > 0) {
                        fullText.append(rowText).append("\n");
                    }
                }
                fullText.append("\n");
            }

            ParsedDocument result = new ParsedDocument();
            result.setContent(fullText.toString());
            result.setSourceType("EXCEL");
            return result;
        }
    }
}
