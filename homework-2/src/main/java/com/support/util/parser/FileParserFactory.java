package com.support.util.parser;

import com.support.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FileParserFactory {

    private final List<FileParser> parsers;
    private final Map<String, FileParser> parserMap;

    public FileParserFactory(List<FileParser> parsers) {
        this.parsers = parsers != null ? parsers : new ArrayList<>();
        this.parserMap = this.parsers.stream()
                .collect(Collectors.toMap(
                        p -> p.getSupportedFormat().toUpperCase(),
                        Function.identity()
                ));
    }

    // Default constructor for Spring
    public FileParserFactory() {
        this.parsers = new ArrayList<>();
        this.parserMap = new HashMap<>();
    }

    public FileParser getParser(String format) {
        if (format == null || format.isEmpty()) {
            throw new ValidationException("File format not specified");
        }

        String normalizedFormat = format.toUpperCase().trim();
        FileParser parser = parserMap.get(normalizedFormat);

        if (parser == null) {
            throw new ValidationException("Unsupported file format: " + format +
                    ". Supported formats: " + String.join(", ", parserMap.keySet()));
        }

        return parser;
    }

    public FileParser detectParser(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new ValidationException("Filename is required for format detection");
        }

        String extension = getFileExtension(filename).toUpperCase();

        switch (extension) {
            case "CSV":
                return parserMap.get("CSV");
            case "JSON":
                return parserMap.get("JSON");
            case "XML":
                return parserMap.get("XML");
            default:
                throw new ValidationException("Cannot detect format from filename: " + filename +
                        ". Supported extensions: .csv, .json, .xml");
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    public List<String> getSupportedFormats() {
        return parsers.stream()
                .map(FileParser::getSupportedFormat)
                .collect(Collectors.toList());
    }
}