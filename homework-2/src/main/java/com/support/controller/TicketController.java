package com.support.controller;

import com.support.domain.ClassificationResult;
import com.support.dto.*;
import com.support.service.ImportService;
import com.support.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ticket Management", description = "APIs for managing customer support tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ImportService importService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new ticket", description = "Creates a new customer support ticket with automatic classification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ticket created successfully",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        log.info("Creating new ticket for customer: {}", request.getCustomerId());
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all tickets", description = "Retrieves a paginated list of tickets with optional filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tickets retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<Page<TicketResponse>> listTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Filter by status (NEW, IN_PROGRESS, WAITING_CUSTOMER, RESOLVED, CLOSED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter by priority")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) String customerId,
            @Parameter(description = "Filter by customer email")
            @RequestParam(required = false) String customerEmail,
            @Parameter(description = "Filter by assigned to")
            @RequestParam(required = false) String assignedTo,
            @Parameter(description = "Search by keyword in subject or description")
            @RequestParam(required = false) String keyword) {

        Map<String, String> filters = new HashMap<>();
        if (status != null) filters.put("status", status);
        if (category != null) filters.put("category", category);
        if (priority != null) filters.put("priority", priority);
        if (customerId != null) filters.put("customerId", customerId);
        if (customerEmail != null) filters.put("customerEmail", customerEmail);
        if (assignedTo != null) filters.put("assignedTo", assignedTo);
        if (keyword != null) filters.put("keyword", keyword);

        log.info("Listing tickets with filters: {}", filters);
        Page<TicketResponse> tickets = ticketService.listTickets(pageable, filters);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID", description = "Retrieves a specific ticket by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket found",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> getTicket(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable UUID id) {
        log.info("Fetching ticket with ID: {}", id);
        TicketResponse response = ticketService.getTicket(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a ticket", description = "Updates an existing ticket with new information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket updated successfully",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> updateTicket(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest request) {
        log.info("Updating ticket with ID: {}", id);
        TicketResponse response = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a ticket", description = "Deletes a ticket permanently")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ticket deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteTicket(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable UUID id) {
        log.info("Deleting ticket with ID: {}", id);
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/auto-classify")
    @Operation(summary = "Auto-classify a ticket", description = "Triggers automatic classification for a ticket")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket classified successfully",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TicketResponse> autoClassifyTicket(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable UUID id) {
        log.info("Auto-classifying ticket with ID: {}", id);
        TicketResponse response = ticketService.autoClassifyTicket(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import tickets in bulk", description = "Imports multiple tickets from CSV, JSON, or XML file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Import completed",
                    content = @Content(schema = @Schema(implementation = BulkImportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "File too large",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Import failed with errors",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BulkImportResponse> importTickets(
            @Parameter(description = "File containing tickets (CSV, JSON, or XML)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "File format (CSV, JSON, XML). Auto-detected if not provided")
            @RequestParam(required = false) String format,
            @Parameter(description = "Only validate without importing")
            @RequestParam(required = false, defaultValue = "false") Boolean validateOnly,
            @Parameter(description = "Import batch identifier")
            @RequestParam(required = false) String importBatch) {

        log.info("Importing tickets from file: {}, format: {}, validateOnly: {}",
                file.getOriginalFilename(), format, validateOnly);

        BulkImportRequest importRequest = BulkImportRequest.builder()
                .file(file)
                .format(format)
                .validateOnly(validateOnly)
                .importBatch(importBatch)
                .build();

        BulkImportResponse response = importService.importTickets(importRequest);

        // Return different status based on import results
        if (response.getFailureCount() == 0) {
            return ResponseEntity.ok(response);
        } else if (response.getSuccessCount() == 0) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
        }
    }
}