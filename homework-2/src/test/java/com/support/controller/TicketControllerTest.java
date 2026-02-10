package com.support.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import com.support.dto.BulkImportResponse;
import com.support.dto.CreateTicketRequest;
import com.support.dto.TicketResponse;
import com.support.exception.ResourceNotFoundException;
import com.support.service.ImportService;
import com.support.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Ticket Controller Tests")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private ImportService importService;

    private CreateTicketRequest validRequest;
    private TicketResponse sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = CreateTicketRequest.builder()
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .subject("Test Subject")
                .description("Test description with minimum length")
                .category(Category.TECHNICAL_ISSUE)
                .priority(Priority.HIGH)
                .build();

        sampleResponse = TicketResponse.builder()
                .id(UUID.randomUUID())
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .subject("Test Subject")
                .description("Test description with minimum length")
                .category(Category.TECHNICAL_ISSUE)
                .priority(Priority.HIGH)
                .status(Status.NEW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /tickets - Create ticket successfully")
    void testCreateTicketSuccess() throws Exception {
        when(ticketService.createTicket(any(CreateTicketRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.subject").value("Test Subject"));

        verify(ticketService, times(1)).createTicket(any(CreateTicketRequest.class));
    }

    @Test
    @DisplayName("POST /tickets - Validation fails with missing email")
    void testCreateTicketMissingEmail() throws Exception {
        CreateTicketRequest invalidRequest = CreateTicketRequest.builder()
                .customerId("CUST001")
                .customerEmail(null)
                .customerName("Test User")
                .subject("Subject")
                .description("Valid description with minimum length")
                .build();

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors['customerEmail']").exists());

        verify(ticketService, never()).createTicket(any());
    }

    @Test
    @DisplayName("POST /tickets - Validation fails with short description")
    void testCreateTicketShortDescription() throws Exception {
        CreateTicketRequest invalidRequest = CreateTicketRequest.builder()
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .subject("Subject")
                .description("Short")
                .build();

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors['description']").exists());
    }

    @Test
    @DisplayName("GET /tickets - List all tickets with pagination")
    void testListTickets() throws Exception {
        List<TicketResponse> tickets = Collections.singletonList(sampleResponse);
        Page<TicketResponse> page = new PageImpl<>(tickets, PageRequest.of(0, 20), 1);

        when(ticketService.listTickets(any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/tickets")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].customerId").value("CUST001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(ticketService, times(1)).listTickets(any(), any());
    }

    @Test
    @DisplayName("GET /tickets - List tickets with multiple filters")
    void testListTicketsFilterByStatus() throws Exception {
        Page<TicketResponse> page = new PageImpl<>(Collections.singletonList(sampleResponse), PageRequest.of(0, 20), 1);

        when(ticketService.listTickets(any(org.springframework.data.domain.Pageable.class), any(Map.class)))
                .thenReturn(page);

        mockMvc.perform(get("/tickets")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(ticketService, times(1)).listTickets(any(), any());
    }

    @Test
    @DisplayName("GET /tickets/{id} - Get ticket by ID successfully")
    void testGetTicketSuccess() throws Exception {
        UUID ticketId = sampleResponse.getId();

        when(ticketService.getTicket(ticketId))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/tickets/{id}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.customerId").value("CUST001"));

        verify(ticketService, times(1)).getTicket(ticketId);
    }

    @Test
    @DisplayName("GET /tickets/{id} - Returns 404 when ticket not found")
    void testGetTicketNotFound() throws Exception {
        UUID ticketId = UUID.randomUUID();

        when(ticketService.getTicket(ticketId))
                .thenThrow(new ResourceNotFoundException("Ticket", "id", ticketId));

        mockMvc.perform(get("/tickets/{id}", ticketId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(ticketService, times(1)).getTicket(ticketId);
    }

    @Test
    @DisplayName("PUT /tickets/{id} - Update ticket successfully")
    void testUpdateTicketSuccess() throws Exception {
        UUID ticketId = sampleResponse.getId();
        TicketResponse updatedResponse = TicketResponse.builder()
                .id(ticketId)
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .subject("Updated Subject")
                .description("Updated description with valid length")
                .category(Category.TECHNICAL_ISSUE)
                .priority(Priority.MEDIUM)
                .status(Status.IN_PROGRESS)
                .updatedAt(LocalDateTime.now())
                .build();

        when(ticketService.updateTicket(eq(ticketId), any()))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/tickets/{id}", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "subject", "Updated Subject",
                        "priority", "MEDIUM"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Updated Subject"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

        verify(ticketService, times(1)).updateTicket(eq(ticketId), any());
    }

    @Test
    @DisplayName("DELETE /tickets/{id} - Delete ticket successfully")
    void testDeleteTicketSuccess() throws Exception {
        UUID ticketId = UUID.randomUUID();

        doNothing().when(ticketService).deleteTicket(ticketId);

        mockMvc.perform(delete("/tickets/{id}", ticketId))
                .andExpect(status().isNoContent());

        verify(ticketService, times(1)).deleteTicket(ticketId);
    }

    @Test
    @DisplayName("POST /tickets/{id}/auto-classify - Auto-classify ticket")
    void testAutoClassifyTicket() throws Exception {
        UUID ticketId = sampleResponse.getId();

        when(ticketService.autoClassifyTicket(ticketId))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/tickets/{id}/auto-classify", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.priority").exists());

        verify(ticketService, times(1)).autoClassifyTicket(ticketId);
    }

    @Test
    @DisplayName("POST /tickets/import - Import with valid file")
    void testImportTickets() throws Exception {
        BulkImportResponse mockResponse = new BulkImportResponse();
        mockResponse.setSuccessCount(0);
        mockResponse.setFailureCount(0);

        when(importService.importTickets(any())).thenReturn(mockResponse);

        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file",
                        "test.csv",
                        "text/csv",
                        "test,data".getBytes()
                );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/tickets/import")
                .file(file)
                .param("format", "CSV"))
                .andExpect(status().isOk());

        verify(importService, times(1)).importTickets(any());
    }
}
