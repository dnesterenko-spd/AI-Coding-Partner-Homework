package com.support.service.impl;

import com.support.domain.*;
import com.support.dto.CreateTicketRequest;
import com.support.dto.TicketResponse;
import com.support.dto.UpdateTicketRequest;
import com.support.exception.ResourceNotFoundException;
import com.support.exception.ValidationException;
import com.support.repository.TicketRepository;
import com.support.service.ClassificationService;
import com.support.service.TicketService;
import com.support.util.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final ClassificationService classificationService;

    @Override
    public TicketResponse createTicket(CreateTicketRequest request) {
        log.debug("Creating new ticket with subject: {}", request.getSubject());

        // Map request to entity
        Ticket ticket = ticketMapper.toEntity(request);

        // Set default values if not provided
        if (ticket.getStatus() == null) {
            ticket.setStatus(Status.NEW);
        }
        if (ticket.getCategory() == null) {
            ticket.setCategory(Category.OTHER);
        }
        if (ticket.getPriority() == null) {
            ticket.setPriority(Priority.MEDIUM);
        }

        // Auto-classify the ticket
        ClassificationResult classification = classificationService.classifyTicket(
                ticket.getSubject(), ticket.getDescription());
        ticket.setClassification(classification);
        ticket.setCategory(classification.getCategory());
        ticket.setPriority(classification.getPriority());

        // Save ticket
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Created ticket with ID: {} - Category: {}, Priority: {}",
                savedTicket.getId(), savedTicket.getCategory(), savedTicket.getPriority());

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID id) {
        log.debug("Fetching ticket with ID: {}", id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        return ticketMapper.toResponse(ticket);
    }

    @Override
    public TicketResponse updateTicket(UUID id, UpdateTicketRequest request) {
        log.debug("Updating ticket with ID: {}", id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        // Update fields
        ticketMapper.updateTicketFromRequest(ticket, request);

        // If status is being changed to resolved/closed, set resolvedAt
        if (request.getStatus() != null &&
                (request.getStatus() == Status.RESOLVED || request.getStatus() == Status.CLOSED)) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }

        // If subject or description changed, reclassify
        if (request.getSubject() != null || request.getDescription() != null) {
            ClassificationResult classification = classificationService.classifyTicket(
                    ticket.getSubject(), ticket.getDescription());
            ticket.setClassification(classification);

            // Only update category/priority if not manually overridden
            if (ticket.getClassification() != null && !ticket.getClassification().getIsManualOverride()) {
                ticket.setCategory(classification.getCategory());
                ticket.setPriority(classification.getPriority());
            }
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Updated ticket with ID: {}", updatedTicket.getId());

        return ticketMapper.toResponse(updatedTicket);
    }

    @Override
    public void deleteTicket(UUID id) {
        log.debug("Deleting ticket with ID: {}", id);

        if (!ticketRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ticket", "id", id);
        }

        ticketRepository.deleteById(id);
        log.info("Deleted ticket with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> listTickets(Pageable pageable, Map<String, String> filters) {
        log.debug("Listing tickets with filters: {}", filters);

        Specification<Ticket> spec = buildSpecification(filters);
        Page<Ticket> tickets = ticketRepository.findAll(spec, pageable);

        return tickets.map(ticketMapper::toResponse);
    }

    @Override
    public TicketResponse autoClassifyTicket(UUID id) {
        log.debug("Auto-classifying ticket with ID: {}", id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        // Reclassify the ticket
        classificationService.reclassifyTicket(ticket);

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Auto-classified ticket {} - Category: {}, Priority: {}",
                id, savedTicket.getCategory(), savedTicket.getPriority());

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        try {
            Status statusEnum = Status.valueOf(status.toUpperCase());
            return ticketRepository.countByStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status value: " + status);
        }
    }

    private Specification<Ticket> buildSpecification(Map<String, String> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by status
            if (filters.containsKey("status")) {
                try {
                    Status status = Status.valueOf(filters.get("status").toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter value: {}", filters.get("status"));
                }
            }

            // Filter by category
            if (filters.containsKey("category")) {
                try {
                    Category category = Category.valueOf(filters.get("category").toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("category"), category));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid category filter value: {}", filters.get("category"));
                }
            }

            // Filter by priority
            if (filters.containsKey("priority")) {
                try {
                    Priority priority = Priority.valueOf(filters.get("priority").toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid priority filter value: {}", filters.get("priority"));
                }
            }

            // Filter by customer ID
            if (filters.containsKey("customerId")) {
                predicates.add(criteriaBuilder.equal(
                        root.get("customer").get("customerId"),
                        filters.get("customerId")
                ));
            }

            // Filter by customer email
            if (filters.containsKey("customerEmail")) {
                predicates.add(criteriaBuilder.equal(
                        root.get("customer").get("customerEmail"),
                        filters.get("customerEmail")
                ));
            }

            // Filter by assigned to
            if (filters.containsKey("assignedTo")) {
                predicates.add(criteriaBuilder.equal(
                        root.get("assignedTo"),
                        filters.get("assignedTo")
                ));
            }

            // Search by keyword in subject or description
            if (filters.containsKey("keyword")) {
                String keyword = "%" + filters.get("keyword").toLowerCase() + "%";
                Predicate subjectPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("subject")), keyword
                );
                Predicate descriptionPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), keyword
                );
                predicates.add(criteriaBuilder.or(subjectPredicate, descriptionPredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}