package com.support.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @Valid
    private Customer customer;

    @NotBlank(message = "Subject is required")
    @Size(min = 1, max = 200, message = "Subject must be between 1 and 200 characters")
    @Column(nullable = false)
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Category category = Category.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "assigned_to")
    private String assignedTo;

    @ElementCollection
    @CollectionTable(name = "ticket_tags",
            joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Embedded
    private TicketMetadata metadata;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "classification_id")
    private ClassificationResult classification;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = Status.NEW;
        }
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
        if (category == null) {
            category = Category.OTHER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == Status.RESOLVED || status == Status.CLOSED) {
            if (resolvedAt == null) {
                resolvedAt = LocalDateTime.now();
            }
        }
    }
}