package org.example.service.parser;

public interface DocumentParser {
    boolean supports(String fileType);
    ParsedDocument parse(java.io.InputStream inputStream, String fileName) throws Exception;
}
