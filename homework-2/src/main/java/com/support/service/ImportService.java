package com.support.service;

import com.support.dto.BulkImportRequest;
import com.support.dto.BulkImportResponse;

public interface ImportService {

    BulkImportResponse importTickets(BulkImportRequest request);
}