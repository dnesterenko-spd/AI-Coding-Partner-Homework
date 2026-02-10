package com.support.service;

import com.support.domain.ClassificationResult;
import com.support.domain.Ticket;

public interface ClassificationService {

    ClassificationResult classifyTicket(Ticket ticket);

    ClassificationResult classifyTicket(String subject, String description);

    void reclassifyTicket(Ticket ticket);

    ClassificationResult manualOverride(Ticket ticket, ClassificationResult override);
}