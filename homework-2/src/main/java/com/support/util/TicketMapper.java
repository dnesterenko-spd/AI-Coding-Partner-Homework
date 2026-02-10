package com.support.util;

import com.support.domain.*;
import com.support.dto.CreateTicketRequest;
import com.support.dto.TicketResponse;
import com.support.dto.UpdateTicketRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer.customerId", source = "customerId")
    @Mapping(target = "customer.customerEmail", source = "customerEmail")
    @Mapping(target = "customer.customerName", source = "customerName")
    @Mapping(target = "metadata.source", source = "source")
    @Mapping(target = "metadata.browser", source = "browser")
    @Mapping(target = "metadata.deviceType", source = "deviceType")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "classification", ignore = true)
    Ticket toEntity(CreateTicketRequest request);

    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "customerEmail", source = "customer.customerEmail")
    @Mapping(target = "customerName", source = "customer.customerName")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "classification", source = "classification")
    TicketResponse toResponse(Ticket ticket);

    @Mapping(target = "source", source = "source")
    @Mapping(target = "browser", source = "browser")
    @Mapping(target = "deviceType", source = "deviceType")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "importBatch", source = "importBatch")
    TicketResponse.TicketMetadataDto toMetadataDto(TicketMetadata metadata);

    @Mapping(target = "category", source = "category")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "confidenceScore", source = "confidenceScore")
    @Mapping(target = "matchedKeywords", source = "matchedKeywords")
    @Mapping(target = "reasoning", source = "reasoning")
    @Mapping(target = "classifiedAt", source = "classifiedAt")
    @Mapping(target = "isManualOverride", source = "isManualOverride")
    TicketResponse.ClassificationResultDto toClassificationDto(ClassificationResult classification);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "classification", ignore = true)
    void updateTicketFromRequest(@MappingTarget Ticket ticket, UpdateTicketRequest request);
}