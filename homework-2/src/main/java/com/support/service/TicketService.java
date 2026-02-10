package com.support.service;

import com.support.domain.Ticket;
import com.support.dto.CreateTicketRequest;
import com.support.dto.TicketResponse;
import com.support.dto.UpdateTicketRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse getTicket(UUID id);

    TicketResponse updateTicket(UUID id, UpdateTicketRequest request);

    void deleteTicket(UUID id);

    Page<TicketResponse> listTickets(Pageable pageable, Map<String, String> filters);

    TicketResponse autoClassifyTicket(UUID id);

    long countByStatus(String status);
}