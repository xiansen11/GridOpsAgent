package org.example.service.parser;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class HtmlDocumentParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(HtmlDocumentParser.class);

    @Override
    public boolean supports(String fileType) {
        return "html".equalsIgnoreCase(fileType) || "htm".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws Exception {
        logger.info("解析HTML文档: {}", fileName);

        org.jsoup.nodes.Document doc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");

        doc.select("script, style, nav, footer, header").remove();

        String text = doc.body() != null ? doc.body().text() : doc.text();

        String structuredText = text.replaceAll("\\s{3,}", "\n\n");

        ParsedDocument result = new ParsedDocument();
        result.setContent(structuredText);
        result.setSourceType("HTML");
        return result;
    }
}
