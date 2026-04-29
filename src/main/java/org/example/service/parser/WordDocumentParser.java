package org.example.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class WordDocumentParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(WordDocumentParser.class);

    @Override
    public boolean supports(String fileType) {
        return "docx".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws Exception {
        logger.info("解析Word文档: {}", fileName);

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuilder fullText = new StringBuilder();

            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    String style = paragraph.getStyle();
                    if (style != null && (style.contains("Heading") || style.contains("标题"))) {
                        fullText.append("\n## ").append(text.trim()).append("\n\n");
                    } else {
                        fullText.append(text.trim()).append("\n\n");
                    }
                }
            }

            ParsedDocument result = new ParsedDocument();
            result.setContent(fullText.toString());
            result.setSourceType("WORD");
            return result;
        }
    }
}
