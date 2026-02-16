package com.support.service;

import com.support.domain.*;
import com.support.dto.CreateTicketRequest;
import com.support.dto.TicketResponse;
import com.support.dto.UpdateTicketRequest;
import com.support.exception.ResourceNotFoundException;
import com.support.repository.TicketRepository;
import com.support.service.impl.TicketServiceImpl;
import com.support.util.TicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ticket Service Tests")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private ClassificationService classificationService;

    private TicketServiceImpl ticketService;
    private Ticket sampleTicket;
    private CreateTicketRequest createRequest;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketRepository, ticketMapper, classificationService);

        sampleTicket = new Ticket();
        sampleTicket.setId(UUID.randomUUID());
        sampleTicket.setCustomer(Customer.builder()
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .build());
        sampleTicket.setSubject("Test Subject");
        sampleTicket.setDescription("Test description with minimum length");
        sampleTicket.setCategory(Category.TECHNICAL_ISSUE);
        sampleTicket.setPriority(Priority.HIGH);
        sampleTicket.setStatus(Status.NEW);
        sampleTicket.setCreatedAt(LocalDateTime.now());
        sampleTicket.setUpdatedAt(LocalDateTime.now());

        createRequest = CreateTicketRequest.builder()
                .customerId("CUST001")
                .customerEmail("test@example.com")
                .customerName("Test User")
                .subject("Test Subject")
                .description("Test description with minimum length")
                .build();
    }

    @Test
    @DisplayName("Create ticket - Should save and classify ticket")
    void testCreateTicket() {
        ClassificationResult classificationResult = ClassificationResult.builder()
                .category(Category.TECHNICAL_ISSUE)
                .priority(Priority.HIGH)
                .confidenceScore(0.85)
                .build();

        when(ticketMapper.toEntity(any(CreateTicketRequest.class))).thenReturn(sampleTicket);
        when(classificationService.classifyTicket(anyString(), anyString())).thenReturn(classificationResult);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(TicketResponse.builder()
                .id(sampleTicket.getId())
                .customerId("CUST001")
                .build());

        TicketResponse response = ticketService.createTicket(createRequest);

        assertNotNull(response);
        assertEquals("CUST001", response.getCustomerId());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(classificationService, times(1)).classifyTicket(anyString(), anyString());
    }

    @Test
    @DisplayName("Get ticket - Should return existing ticket")
    void testGetTicketSuccess() {
        UUID ticketId = sampleTicket.getId();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));
        when(ticketMapper.toResponse(sampleTicket)).thenReturn(TicketResponse.builder()
                .id(ticketId)
                .customerId("CUST001")
                .build());

        TicketResponse response = ticketService.getTicket(ticketId);

        assertNotNull(response);
        assertEquals("CUST001", response.getCustomerId());
        verify(ticketRepository, times(1)).findById(ticketId);
    }

    @Test
    @DisplayName("Get ticket - Should throw exception when not found")
    void testGetTicketNotFound() {
        UUID ticketId = UUID.randomUUID();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicket(ticketId));
        verify(ticketRepository, times(1)).findById(ticketId);
    }

    @Test
    @DisplayName("Update ticket - Should update existing ticket")
    void testUpdateTicket() {
        UUID ticketId = sampleTicket.getId();
        UpdateTicketRequest updateRequest = UpdateTicketRequest.builder()
                .subject("Updated Subject")
                .priority(Priority.MEDIUM)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));
        doNothing().when(ticketMapper).updateTicketFromRequest(any(Ticket.class), any(UpdateTicketRequest.class));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(TicketResponse.builder()
                .id(ticketId)
                .subject("Updated Subject")
                .build());

        TicketResponse response = ticketService.updateTicket(ticketId, updateRequest);

        assertNotNull(response);
        verify(ticketRepository, times(1)).findById(ticketId);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Delete ticket - Should delete existing ticket")
    void testDeleteTicket() {
        UUID ticketId = UUID.randomUUID();

        when(ticketRepository.existsById(ticketId)).thenReturn(true);
        doNothing().when(ticketRepository).deleteById(ticketId);

        assertDoesNotThrow(() -> ticketService.deleteTicket(ticketId));
        verify(ticketRepository, times(1)).deleteById(ticketId);
    }

    @Test
    @DisplayName("Delete ticket - Should throw exception when not found")
    void testDeleteTicketNotFound() {
        UUID ticketId = UUID.randomUUID();

        when(ticketRepository.existsById(ticketId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> ticketService.deleteTicket(ticketId));
        verify(ticketRepository, never()).deleteById(ticketId);
    }

    @Test
    @DisplayName("List tickets - Should return paginated results")
    void testListTickets() {
        List<Ticket> tickets = Collections.singletonList(sampleTicket);
        Page<Ticket> page = new PageImpl<>(tickets, PageRequest.of(0, 20), 1);

        when(ticketRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(TicketResponse.builder()
                .id(sampleTicket.getId())
                .build());

        Page<TicketResponse> response = ticketService.listTickets(PageRequest.of(0, 20), new HashMap<>());

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(ticketRepository, times(1)).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Auto-classify ticket - Should reclassify and update")
    void testAutoClassifyTicket() {
        UUID ticketId = sampleTicket.getId();
        ClassificationResult newClassification = ClassificationResult.builder()
                .category(Category.BUG_REPORT)
                .priority(Priority.URGENT)
                .confidenceScore(0.92)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));
        doNothing().when(classificationService).reclassifyTicket(any(Ticket.class));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(TicketResponse.builder()
                .id(ticketId)
                .category(Category.BUG_REPORT)
                .build());

        TicketResponse response = ticketService.autoClassifyTicket(ticketId);

        assertNotNull(response);
        verify(classificationService, times(1)).reclassifyTicket(any(Ticket.class));
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Count by status - Should return count for given status")
    void testCountByStatus() {
        when(ticketRepository.countByStatus(Status.NEW)).thenReturn(5L);

        long count = ticketService.countByStatus("NEW");

        assertEquals(5L, count);
        verify(ticketRepository, times(1)).countByStatus(Status.NEW);
    }
}
