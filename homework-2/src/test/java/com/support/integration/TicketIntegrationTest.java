package com.support.integration;

import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import com.support.domain.Ticket;
import com.support.dto.CreateTicketRequest;
import com.support.dto.TicketResponse;
import com.support.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Ticket Integration Tests")
class TicketIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration - Create and retrieve ticket")
    void testCreateAndRetrieveTicket() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .subject("Integration Test Subject")
                .description("This is an integration test with valid description length")
                .build();

        // Create ticket
        var result = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String ticketJson = result.getResponse().getContentAsString();
        TicketResponse createdTicket = objectMapper.readValue(ticketJson, TicketResponse.class);

        // Retrieve ticket
        mockMvc.perform(get("/tickets/{id}", createdTicket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.subject").value("Integration Test Subject"));
    }

    @Test
    @DisplayName("Integration - Update ticket and verify changes")
    void testUpdateTicketFlow() throws Exception {
        // Create ticket first
        CreateTicketRequest createRequest = CreateTicketRequest.builder()
                .customerId("CUST002")
                .customerEmail("update@example.com")
                .customerName("Update Test User")
                .subject("Original Subject")
                .description("Original description with minimum valid length")
                .build();

        var createResult = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String ticketJson = createResult.getResponse().getContentAsString();
        TicketResponse createdTicket = objectMapper.readValue(ticketJson, TicketResponse.class);

        // Update ticket
        var updateRequest = objectMapper.createObjectNode();
        updateRequest.put("subject", "Updated Subject");
        updateRequest.put("priority", "HIGH");

        mockMvc.perform(put("/tickets/{id}", createdTicket.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Updated Subject"));
    }

    @Test
    @DisplayName("Integration - List tickets with filtering")
    void testListTicketsWithFilters() throws Exception {
        // Create multiple tickets
        for (int i = 0; i < 3; i++) {
            CreateTicketRequest request = CreateTicketRequest.builder()
                    .customerId("CUST" + i)
                    .customerEmail("user" + i + "@example.com")
                    .customerName("User " + i)
                    .subject("Subject " + i)
                    .description("Description " + i + " with valid length for integration test")
                    .build();

            mockMvc.perform(post("/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // List all tickets
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));

        // List with pagination
        mockMvc.perform(get("/tickets")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("Integration - Delete ticket")
    void testDeleteTicket() throws Exception {
        // Create ticket
        CreateTicketRequest request = CreateTicketRequest.builder()
                .customerId("CUST_DEL")
                .customerEmail("delete@example.com")
                .customerName("Delete Test")
                .subject("To be deleted")
                .description("This ticket will be deleted from the system")
                .build();

        var result = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String ticketJson = result.getResponse().getContentAsString();
        TicketResponse createdTicket = objectMapper.readValue(ticketJson, TicketResponse.class);

        // Delete ticket
        mockMvc.perform(delete("/tickets/{id}", createdTicket.getId()))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/tickets/{id}", createdTicket.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration - Auto-classification on creation")
    void testAutoClassificationOnCreation() throws Exception {
        // Create ticket with keywords that trigger classification
        CreateTicketRequest request = CreateTicketRequest.builder()
                .customerId("CUST_CLASS")
                .customerEmail("classify@example.com")
                .customerName("Classification Test")
                .subject("Production system down critical")
                .description("The production system is completely down affecting all users urgently")
                .build();

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.priority").exists());
    }
}
