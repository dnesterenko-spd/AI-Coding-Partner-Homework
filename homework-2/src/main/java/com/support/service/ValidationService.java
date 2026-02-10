package com.support.service;

import com.support.dto.CreateTicketRequest;

import java.util.Map;

public interface ValidationService {

    void validateTicketData(CreateTicketRequest request);

    void validateImportRecord(Map<String, String> record);

    boolean isValidEmail(String email);

    boolean isValidEnumValue(String value, Class<? extends Enum<?>> enumClass);

    Map<String, String> validateImportBatch(Map<String, String>[] records);
}