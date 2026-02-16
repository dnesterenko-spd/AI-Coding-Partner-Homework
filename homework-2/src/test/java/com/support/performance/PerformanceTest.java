package com.support.performance;

import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import com.support.domain.Ticket;
import com.support.domain.Customer;
import com.support.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Performance Tests")
class PerformanceTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Performance - Create 1000 tickets in bulk")
    void testBulkCreate1000Tickets() {
        List<Ticket> tickets = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            Ticket ticket = new Ticket();
            ticket.setId(UUID.randomUUID());
            ticket.setCustomer(Customer.builder()
                    .customerId("PERF_CUST_" + i)
                    .customerEmail("perf" + i + "@example.com")
                    .customerName("Performance Test User " + i)
                    .build());
            ticket.setSubject("Performance Test Subject " + i);
            ticket.setDescription("This is a performance test ticket number " + i + " with valid length description");
            ticket.setCategory(Category.TECHNICAL_ISSUE);
            ticket.setPriority(Priority.MEDIUM);
            ticket.setStatus(Status.NEW);
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setUpdatedAt(LocalDateTime.now());

            tickets.add(ticket);
        }

        ticketRepository.saveAll(tickets);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        long count = ticketRepository.count();
        assertEquals(1000, count);

        // Performance assertion: should complete in reasonable time
        assertTrue(duration < 30000, "Bulk insert of 1000 records should complete in less than 30 seconds");
    }

    @Test
    @DisplayName("Performance - Query with pagination (1000 records)")
    void testPaginationPerformance() {
        createTestRecords(1000);

        long startTime = System.currentTimeMillis();

        Page<Ticket> firstPage = ticketRepository.findAll(PageRequest.of(0, 20));
        Page<Ticket> lastPage = ticketRepository.findAll(PageRequest.of(49, 20));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertEquals(20, firstPage.getContent().size());
        assertEquals(20, lastPage.getContent().size());

        // Should query efficiently
        assertTrue(duration < 5000, "Pagination queries should be fast");
    }

    @Test
    @DisplayName("Performance - Filter by status (1000 records)")
    void testFilterPerformance() {
        createTestRecords(1000);

        long startTime = System.currentTimeMillis();

        List<Ticket> newTickets = ticketRepository.findByStatus(Status.NEW);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(newTickets.size() > 0);

        // Should filter efficiently
        assertTrue(duration < 5000, "Status filter should be fast");
    }

    @Test
    @DisplayName("Performance - Search by keyword in 1000 records")
    void testSearchPerformance() {
        createTestRecords(1000);

        long startTime = System.currentTimeMillis();

        Page<Ticket> results = ticketRepository.searchByKeyword("Test", PageRequest.of(0, 20));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(results.getTotalElements() > 0);

        // Search should be reasonably fast
        assertTrue(duration < 10000, "Keyword search should be fast");
    }

    @Test
    @DisplayName("Performance - Count by status (1000 records)")
    void testCountPerformance() {
        createTestRecords(1000);

        long startTime = System.currentTimeMillis();

        long newCount = ticketRepository.countByStatus(Status.NEW);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertEquals(1000, newCount);

        // Count should be very fast
        assertTrue(duration < 2000, "Count operation should be very fast");
    }

    private void createTestRecords(int count) {
        List<Ticket> tickets = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Ticket ticket = new Ticket();
            ticket.setId(UUID.randomUUID());
            ticket.setCustomer(Customer.builder()
                    .customerId("PERF_CUST_" + i)
                    .customerEmail("perf" + i + "@example.com")
                    .customerName("Test User " + i)
                    .build());
            ticket.setSubject("Test Subject " + i);
            ticket.setDescription("Test description for performance testing record number " + i);
            ticket.setCategory(Category.TECHNICAL_ISSUE);
            ticket.setPriority(Priority.MEDIUM);
            ticket.setStatus(Status.NEW);
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setUpdatedAt(LocalDateTime.now());

            tickets.add(ticket);
        }

        ticketRepository.saveAll(tickets);
    }
}
