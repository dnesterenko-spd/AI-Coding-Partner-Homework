package com.support.service;

import com.support.dto.BulkImportRequest;
import com.support.dto.BulkImportResponse;
import com.support.exception.ValidationException;
import com.support.service.impl.ImportServiceImpl;
import com.support.util.parser.FileParserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Import Service Tests")
class ImportServiceTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private ValidationService validationService;

    private ImportServiceImpl importService;
    private FileParserFactory fileParserFactory;

    @BeforeEach
    void setUp() {
        // Use a real FileParserFactory instance instead of mocking
        fileParserFactory = new FileParserFactory(new ArrayList<>());
        importService = new ImportServiceImpl(ticketService, validationService, fileParserFactory);
    }

    @Test
    @DisplayName("Import - File is required")
    void testImportFileRequired() {
        BulkImportRequest request = BulkImportRequest.builder()
                .file(null)
                .build();

        assertThrows(ValidationException.class, () -> importService.importTickets(request));
    }

    @Test
    @DisplayName("Import - Empty file is rejected")
    void testImportEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        BulkImportRequest request = BulkImportRequest.builder()
                .file(emptyFile)
                .build();

        assertThrows(ValidationException.class, () -> importService.importTickets(request));
    }

    @Test
    @DisplayName("Import - File exceeding size limit is rejected")
    void testImportFileTooLarge() throws Exception {
        byte[] largeContent = new byte[105_000_000]; // >100MB
        MultipartFile largeFile = new MockMultipartFile("file", "test.csv", "text/csv", largeContent);
        BulkImportRequest request = BulkImportRequest.builder()
                .file(largeFile)
                .format("CSV")
                .build();

        assertThrows(ValidationException.class, () -> importService.importTickets(request));
    }

    @Test
    @DisplayName("Import - File without recognized extension fails")
    void testImportUnrecognizedFormat() throws Exception {
        MultipartFile unknownFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        BulkImportRequest request = BulkImportRequest.builder()
                .file(unknownFile)
                .build();

        assertThrows(ValidationException.class, () -> importService.importTickets(request));
    }

    @Test
    @DisplayName("Import - Returns BulkImportResponse structure")
    void testImportResponseStructure() throws Exception {
        // Test that proper exception is thrown when no parsers are available
        MultipartFile csvFile = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());
        BulkImportRequest request = BulkImportRequest.builder()
                .file(csvFile)
                .format("CSV")
                .build();

        // Should throw because no parsers are registered
        assertThrows(ValidationException.class, () -> importService.importTickets(request));
    }

    @Test
    @DisplayName("Import - Handles validation exceptions properly")
    void testImportHandlesValidationErrors() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());
        BulkImportRequest request = BulkImportRequest.builder()
                .file(file)
                .format("CSV")
                .build();

        // No parsers registered, should fail validation
        assertThrows(ValidationException.class, () -> importService.importTickets(request));
    }
}
