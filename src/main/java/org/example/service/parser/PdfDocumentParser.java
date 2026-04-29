package org.example.service.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class PdfDocumentParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(PdfDocumentParser.class);

    @Override
    public boolean supports(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws Exception {
        logger.info("解析PDF文档: {}", fileName);

        Path tempFile = Files.createTempFile("pdf_", ".pdf");
        Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(tempFile.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int totalPages = document.getNumberOfPages();
            StringBuilder fullText = new StringBuilder();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);
                fullText.append("--- 第").append(i).append("页 ---\n");
                fullText.append(pageText).append("\n\n");
            }

            ParsedDocument result = new ParsedDocument();
            result.setContent(fullText.toString());
            result.setTotalPages(totalPages);
            result.setSourceType("PDF");

            Files.deleteIfExists(tempFile);
            return result;
        }
    }
}
