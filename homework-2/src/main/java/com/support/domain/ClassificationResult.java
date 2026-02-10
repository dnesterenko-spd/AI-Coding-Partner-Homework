package com.support.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "classification_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private Double confidenceScore;

    @ElementCollection
    @CollectionTable(name = "classification_keywords",
            joinColumns = @JoinColumn(name = "classification_id"))
    @Column(name = "keyword")
    @Builder.Default
    private List<String> matchedKeywords = new ArrayList<>();

    private String reasoning;

    private LocalDateTime classifiedAt;

    private Boolean isManualOverride;

    @PrePersist
    protected void onCreate() {
        if (classifiedAt == null) {
            classifiedAt = LocalDateTime.now();
        }
        if (isManualOverride == null) {
            isManualOverride = false;
        }
    }
}