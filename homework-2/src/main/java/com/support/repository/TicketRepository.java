package com.support.repository;

import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import com.support.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByStatus(Status status);

    List<Ticket> findByCategory(Category category);

    List<Ticket> findByPriority(Priority priority);

    List<Ticket> findByCustomerCustomerId(String customerId);

    List<Ticket> findByCustomerCustomerEmail(String customerEmail);

    Page<Ticket> findByStatusIn(List<Status> statuses, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Ticket> findTicketsCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Ticket t WHERE t.assignedTo = :assignedTo AND t.status IN :statuses")
    List<Ticket> findByAssignedToAndStatusIn(@Param("assignedTo") String assignedTo,
                                            @Param("statuses") List<Status> statuses);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    Long countByStatus(@Param("status") Status status);

    @Query("SELECT t FROM Ticket t WHERE LOWER(t.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Ticket> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.metadata.importBatch = :importBatch")
    List<Ticket> findByImportBatch(@Param("importBatch") String importBatch);
}