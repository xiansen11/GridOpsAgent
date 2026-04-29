package org.example.service.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class TextDocumentParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(TextDocumentParser.class);

    @Override
    public boolean supports(String fileType) {
        return "txt".equalsIgnoreCase(fileType) || "md".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws Exception {
        logger.info("解析文本文档: {}", fileName);

        String fileType = fileName != null && fileName.toLowerCase().endsWith(".md") ? "md" : "txt";

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));

            ParsedDocument result = new ParsedDocument();
            result.setContent(content);
            result.setSourceType("md".equalsIgnoreCase(fileType) ? "MARKDOWN" : "TEXT");
            return result;
        }
    }
}
