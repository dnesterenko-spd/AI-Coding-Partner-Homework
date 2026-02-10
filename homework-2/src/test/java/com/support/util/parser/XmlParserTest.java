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

@DisplayName("XML Parser Tests")
class XmlParserTest {

    private XmlParser xmlParser;

    @BeforeEach
    void setUp() {
        xmlParser = new XmlParser();
    }

    @Test
    @DisplayName("Parse - Valid XML file with required fields")
    void testParseValidXml() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<tickets>" +
                "<ticket>" +
                "<customer_id>CUST001</customer_id>" +
                "<customer_email>user@example.com</customer_email>" +
                "<customer_name>John Doe</customer_name>" +
                "<subject>Test Subject</subject>" +
                "<description>Valid description with minimum length</description>" +
                "</ticket>" +
                "</tickets>";

        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
        List<CreateTicketRequest> tickets = xmlParser.parse(inputStream);

        assertEquals(1, tickets.size());
        assertEquals("CUST001", tickets.get(0).getCustomerId());
        assertEquals("user@example.com", tickets.get(0).getCustomerEmail());
    }

    @Test
    @DisplayName("Parse - XML with optional fields")
    void testParseXmlWithOptionalFields() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<tickets>" +
                "<ticket>" +
                "<customer_id>CUST001</customer_id>" +
                "<customer_email>user@example.com</customer_email>" +
                "<customer_name>John Doe</customer_name>" +
                "<subject>Subject</subject>" +
                "<description>Valid description</description>" +
                "<category>ACCOUNT_ACCESS</category>" +
                "<priority>HIGH</priority>" +
                "</ticket>" +
                "</tickets>";

        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
        List<CreateTicketRequest> tickets = xmlParser.parse(inputStream);

        assertEquals(1, tickets.size());
        assertNotNull(tickets.get(0).getCategory());
        assertNotNull(tickets.get(0).getPriority());
    }

    @Test
    @DisplayName("Parse - XML with multiple records")
    void testParseMultipleXmlRecords() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<tickets>" +
                "<ticket>" +
                "<customer_id>CUST001</customer_id>" +
                "<customer_email>user1@example.com</customer_email>" +
                "<customer_name>User One</customer_name>" +
                "<subject>Subject 1</subject>" +
                "<description>Valid description 1</description>" +
                "</ticket>" +
                "<ticket>" +
                "<customer_id>CUST002</customer_id>" +
                "<customer_email>user2@example.com</customer_email>" +
                "<customer_name>User Two</customer_name>" +
                "<subject>Subject 2</subject>" +
                "<description>Valid description 2</description>" +
                "</ticket>" +
                "</tickets>";

        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
        List<CreateTicketRequest> tickets = xmlParser.parse(inputStream);

        assertEquals(2, tickets.size());
    }

    @Test
    @DisplayName("Parse - XML missing required element throws exception")
    void testParseXmlMissingRequiredElement() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<tickets>" +
                "<ticket>" +
                "<customer_id>CUST001</customer_id>" +
                "<customer_email>user@example.com</customer_email>" +
                "<customer_name>John Doe</customer_name>" +
                "<subject>Subject</subject>" +
                "</ticket>" +
                "</tickets>";

        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());

        assertThrows(ImportException.class, () -> xmlParser.parse(inputStream));
    }

    @Test
    @DisplayName("Parse - Malformed XML throws exception")
    void testParseInvalidXmlFormat() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<tickets>" +
                "<ticket>" +
                "<customer_id>CUST001</unclosed>";

        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());

        assertThrows(ImportException.class, () -> xmlParser.parse(inputStream));
    }

    @Test
    @DisplayName("Parse - Empty XML throws exception")
    void testParseEmptyXml() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<tickets></tickets>";

        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());

        assertThrows(ImportException.class, () -> xmlParser.parse(inputStream));
    }

    @Test
    @DisplayName("Parse - Supported format returns XML")
    void testSupportedFormat() {
        assertEquals("XML", xmlParser.getSupportedFormat());
    }

    @Test
    @DisplayName("Parse - Empty required element throws exception")
    void testParseEmptyRequiredElement() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<tickets>" +
                "<ticket>" +
                "<customer_id></customer_id>" +
                "<customer_email>user@example.com</customer_email>" +
                "<customer_name>John Doe</customer_name>" +
                "<subject>Subject</subject>" +
                "<description>Valid description</description>" +
                "</ticket>" +
                "</tickets>";

        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());

        assertThrows(ImportException.class, () -> xmlParser.parse(inputStream));
    }
}
