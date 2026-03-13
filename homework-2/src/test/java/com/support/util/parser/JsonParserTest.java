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

@DisplayName("JSON Parser Tests")
class JsonParserTest {

    private JsonParser jsonParser;

    @BeforeEach
    void setUp() {
        jsonParser = new JsonParser();
    }

    @Test
    @DisplayName("Parse - Valid JSON array format")
    void testParseValidJsonArray() throws Exception {
        String jsonContent = "[" +
                "{\"customer_id\":\"CUST001\",\"customer_email\":\"user@example.com\"," +
                "\"customer_name\":\"John Doe\",\"subject\":\"Subject\",\"description\":\"Valid description\"}" +
                "]";

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());
        List<CreateTicketRequest> tickets = jsonParser.parse(inputStream);

        assertEquals(1, tickets.size());
        assertEquals("CUST001", tickets.get(0).getCustomerId());
    }

    @Test
    @DisplayName("Parse - Valid JSON with tickets object wrapper")
    void testParseValidJsonWithWrapper() throws Exception {
        String jsonContent = "{\"tickets\":[" +
                "{\"customer_id\":\"CUST001\",\"customer_email\":\"user@example.com\"," +
                "\"customer_name\":\"John Doe\",\"subject\":\"Subject\",\"description\":\"Valid description\"}" +
                "]}";

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());
        List<CreateTicketRequest> tickets = jsonParser.parse(inputStream);

        assertEquals(1, tickets.size());
    }

    @Test
    @DisplayName("Parse - JSON with optional fields")
    void testParseJsonWithOptionalFields() throws Exception {
        String jsonContent = "{\"tickets\":[" +
                "{\"customer_id\":\"CUST001\",\"customer_email\":\"user@example.com\"," +
                "\"customer_name\":\"John Doe\",\"subject\":\"Subject\",\"description\":\"Valid description\"," +
                "\"category\":\"ACCOUNT_ACCESS\",\"priority\":\"HIGH\"}" +
                "]}";

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());
        List<CreateTicketRequest> tickets = jsonParser.parse(inputStream);

        assertEquals(1, tickets.size());
        assertNotNull(tickets.get(0).getCategory());
    }

    @Test
    @DisplayName("Parse - JSON with multiple records")
    void testParseMultipleJsonRecords() throws Exception {
        String jsonContent = "[" +
                "{\"customer_id\":\"CUST001\",\"customer_email\":\"user1@example.com\"," +
                "\"customer_name\":\"User 1\",\"subject\":\"Subject 1\",\"description\":\"Valid description 1\"}," +
                "{\"customer_id\":\"CUST002\",\"customer_email\":\"user2@example.com\"," +
                "\"customer_name\":\"User 2\",\"subject\":\"Subject 2\",\"description\":\"Valid description 2\"}" +
                "]";

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());
        List<CreateTicketRequest> tickets = jsonParser.parse(inputStream);

        assertEquals(2, tickets.size());
    }

    @Test
    @DisplayName("Parse - Invalid JSON format throws exception")
    void testParseInvalidJsonFormat() throws Exception {
        String jsonContent = "{\"tickets\": invalid json}";

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

        assertThrows(ImportException.class, () -> jsonParser.parse(inputStream));
    }

    @Test
    @DisplayName("Parse - Missing required field throws exception")
    void testParseMissingRequiredField() throws Exception {
        String jsonContent = "[" +
                "{\"customer_id\":\"CUST001\",\"customer_email\":\"user@example.com\"," +
                "\"customer_name\":\"John Doe\",\"subject\":\"Subject\"}" +
                "]";

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

        assertThrows(ImportException.class, () -> jsonParser.parse(inputStream));
    }

    @Test
    @DisplayName("Parse - Null values in required fields throw exception")
    void testParseNullRequiredField() throws Exception {
        String jsonContent = "[" +
                "{\"customer_id\":\"CUST001\",\"customer_email\":null," +
                "\"customer_name\":\"John Doe\",\"subject\":\"Subject\",\"description\":\"Valid description\"}" +
                "]";

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

        assertThrows(ImportException.class, () -> jsonParser.parse(inputStream));
    }

    @Test
    @DisplayName("Parse - Supported format returns JSON")
    void testSupportedFormat() {
        assertEquals("JSON", jsonParser.getSupportedFormat());
    }

    @Test
    @DisplayName("Parse - Empty JSON array returns empty list")
    void testParseEmptyJsonArray() throws Exception {
        String jsonContent = "[]";

        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());
        List<CreateTicketRequest> tickets = jsonParser.parse(inputStream);

        assertEquals(0, tickets.size());
    }
}
