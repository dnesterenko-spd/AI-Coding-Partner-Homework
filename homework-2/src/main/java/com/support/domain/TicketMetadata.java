package com.support.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMetadata {

    private String source;
    private String browser;
    private String deviceType;
    private String ipAddress;
    private String importBatch;
}