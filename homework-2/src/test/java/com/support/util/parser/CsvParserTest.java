package com.support.util.parser;

import com.support.dto.CreateTicketRequest;
import com.support.exception.ImportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CSV Parser Tests")
class CsvParserTest {

    private CsvParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new CsvParser();
    }

    @Test
    @DisplayName("Parse - Valid CSV file with required fields")
    void testParseValidCsv() throws Exception {
        String csvContent = "customer_id,customer_email,customer_name,subject,description\n" +
                "CUST001,user@example.com,John Doe,Test Subject,This is a valid description with minimum length";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<CreateTicketRequest> tickets = csvParser.parse(inputStream);

        assertEquals(1, tickets.size());
        assertEquals("CUST001", tickets.get(0).getCustomerId());
        assertEquals("user@example.com", tickets.get(0).getCustomerEmail());
    }

    @Test
    @DisplayName("Parse - CSV with optional fields")
    void testParseWithOptionalFields() throws Exception {
        String csvContent = "customer_id,customer_email,customer_name,subject,description,category,priority\n" +
                "CUST001,user@example.com,John Doe,Test Subject,Valid description here,ACCOUNT_ACCESS,HIGH";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<CreateTicketRequest> tickets = csvParser.parse(inputStream);

        assertEquals(1, tickets.size());
        assertNotNull(tickets.get(0).getCategory());
        assertNotNull(tickets.get(0).getPriority());
    }

    @Test
    @DisplayName("Parse - CSV with multiple records")
    void testParseMultipleRecords() throws Exception {
        String csvContent = "customer_id,customer_email,customer_name,subject,description\n" +
                "CUST001,user1@example.com,User One,Subject 1,Valid description for first record\n" +
                "CUST002,user2@example.com,User Two,Subject 2,Valid description for second record\n" +
                "CUST003,user3@example.com,User Three,Subject 3,Valid description for third record";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<CreateTicketRequest> tickets = csvParser.parse(inputStream);

        assertEquals(3, tickets.size());
    }

    @Test
    @DisplayName("Parse - CSV missing required header")
    void testParseMissingHeader() throws Exception {
        String csvContent = "customer_id,customer_name,subject,description\n" +
                "CUST001,John Doe,Test Subject,Valid description";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        assertThrows(ImportException.class, () -> csvParser.parse(inputStream));
    }

    @Test
    @DisplayName("Parse - CSV with missing required field value")
    void testParseMissingFieldValue() throws Exception {
        String csvContent = "customer_id,customer_email,customer_name,subject,description\n" +
                "CUST001,,John Doe,Test Subject,Valid description";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        assertThrows(ImportException.class, () -> csvParser.parse(inputStream));
    }

    @Test
    @DisplayName("Parse - CSV with invalid enum value")
    void testParseInvalidEnumValue() throws Exception {
        String csvContent = "customer_id,customer_email,customer_name,subject,description,category\n" +
                "CUST001,user@example.com,John Doe,Test Subject,Valid description,INVALID_CATEGORY";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<CreateTicketRequest> tickets = csvParser.parse(inputStream);

        // Should not throw but skip invalid enum value
        assertEquals(1, tickets.size());
        assertNull(tickets.get(0).getCategory());
    }

    @Test
    @DisplayName("Parse - Supported format returns CSV")
    void testSupportedFormat() {
        assertEquals("CSV", csvParser.getSupportedFormat());
    }

    @Test
    @DisplayName("Parse - Case-insensitive header matching")
    void testCaseInsensitiveHeaders() throws Exception {
        String csvContent = "Customer_ID,Customer_Email,Customer_Name,Subject,Description\n" +
                "CUST001,user@example.com,John Doe,Test Subject,Valid description";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<CreateTicketRequest> tickets = csvParser.parse(inputStream);

        assertEquals(1, tickets.size());
    }

    @Test
    @DisplayName("Parse - Empty CSV throws exception")
    void testParseEmptyCsv() throws Exception {
        String csvContent = "";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        assertThrows(ImportException.class, () -> csvParser.parse(inputStream));
    }
}
