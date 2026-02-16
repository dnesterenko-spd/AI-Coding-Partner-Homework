package com.support.util.parser;

import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import com.support.dto.CreateTicketRequest;
import com.support.exception.ImportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

@Component
@Slf4j
public class XmlParser implements FileParser {

    @Override
    public List<CreateTicketRequest> parse(InputStream inputStream) throws Exception {
        List<CreateTicketRequest> tickets = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            // Look for tickets root element or individual ticket elements
            NodeList ticketNodes = document.getElementsByTagName("ticket");
            if (ticketNodes.getLength() == 0) {
                // Try alternative naming
                ticketNodes = document.getElementsByTagName("Ticket");
            }
            if (ticketNodes.getLength() == 0) {
                // Look for tickets container
                NodeList ticketsContainer = document.getElementsByTagName("tickets");
                if (ticketsContainer.getLength() > 0) {
                    Element container = (Element) ticketsContainer.item(0);
                    ticketNodes = container.getElementsByTagName("ticket");
                }
            }

            if (ticketNodes.getLength() == 0) {
                throw new ImportException("No ticket elements found in XML file");
            }

            for (int i = 0; i < ticketNodes.getLength(); i++) {
                try {
                    Node ticketNode = ticketNodes.item(i);
                    if (ticketNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element ticketElement = (Element) ticketNode;
                        CreateTicketRequest ticket = parseTicketElement(ticketElement);
                        tickets.add(ticket);
                    }
                } catch (Exception e) {
                    String errorMsg = String.format("Ticket %d: %s", i + 1, e.getMessage());
                    errors.add(errorMsg);
                    log.warn("Failed to parse XML ticket at position {}: {}", i + 1, e.getMessage());
                }
            }

            if (!errors.isEmpty() && tickets.isEmpty()) {
                throw new ImportException("All records failed validation", errors,
                        ticketNodes.getLength(), 0);
            }

            log.info("Successfully parsed {} tickets from XML", tickets.size());
            return tickets;

        } catch (ImportException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing XML file: {}", e.getMessage());
            throw new ImportException("Failed to parse XML file: " + e.getMessage(), e);
        }
    }

    private CreateTicketRequest parseTicketElement(Element ticketElement) {
        CreateTicketRequest.CreateTicketRequestBuilder builder = CreateTicketRequest.builder();

        // Required fields
        builder.customerId(getRequiredElementText(ticketElement, "customer_id", "customerId"));
        builder.customerEmail(getRequiredElementText(ticketElement, "customer_email", "customerEmail"));
        builder.customerName(getRequiredElementText(ticketElement, "customer_name", "customerName"));
        builder.subject(getRequiredElementText(ticketElement, "subject"));
        builder.description(getRequiredElementText(ticketElement, "description"));

        // Optional fields
        String category = getOptionalElementText(ticketElement, "category");
        if (category != null && !category.isEmpty()) {
            try {
                builder.category(Category.valueOf(category.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid category value: {}, using default", category);
            }
        }

        String priority = getOptionalElementText(ticketElement, "priority");
        if (priority != null && !priority.isEmpty()) {
            try {
                builder.priority(Priority.valueOf(priority.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid priority value: {}, using default", priority);
            }
        }

        String status = getOptionalElementText(ticketElement, "status");
        if (status != null && !status.isEmpty()) {
            try {
                builder.status(Status.valueOf(status.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid status value: {}, using default", status);
            }
        }

        builder.assignedTo(getOptionalElementText(ticketElement, "assigned_to", "assignedTo"));
        builder.source(getOptionalElementText(ticketElement, "source"));
        builder.browser(getOptionalElementText(ticketElement, "browser"));
        builder.deviceType(getOptionalElementText(ticketElement, "device_type", "deviceType"));

        // Parse tags if present
        NodeList tagNodes = ticketElement.getElementsByTagName("tags");
        if (tagNodes.getLength() > 0) {
            Set<String> tags = new HashSet<>();
            Element tagsElement = (Element) tagNodes.item(0);

            // Check for individual tag elements
            NodeList tagElements = tagsElement.getElementsByTagName("tag");
            if (tagElements.getLength() > 0) {
                for (int i = 0; i < tagElements.getLength(); i++) {
                    String tag = tagElements.item(i).getTextContent().trim();
                    if (!tag.isEmpty()) {
                        tags.add(tag);
                    }
                }
            } else {
                // Handle comma-separated tags
                String tagsText = tagsElement.getTextContent().trim();
                if (!tagsText.isEmpty()) {
                    for (String tag : tagsText.split(",")) {
                        tags.add(tag.trim());
                    }
                }
            }
            builder.tags(tags);
        }

        return builder.build();
    }

    private String getRequiredElementText(Element parent, String... tagNames) {
        String value = getOptionalElementText(parent, tagNames);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required field: " + tagNames[0]);
        }
        return value;
    }

    private String getOptionalElementText(Element parent, String... tagNames) {
        for (String tagName : tagNames) {
            NodeList nodes = parent.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                String text = nodes.item(0).getTextContent();
                return text != null ? text.trim() : null;
            }
        }

        // Try attributes as well
        for (String tagName : tagNames) {
            String attrValue = parent.getAttribute(tagName);
            if (attrValue != null && !attrValue.isEmpty()) {
                return attrValue.trim();
            }
        }

        return null;
    }

    @Override
    public String getSupportedFormat() {
        return "XML";
    }
}