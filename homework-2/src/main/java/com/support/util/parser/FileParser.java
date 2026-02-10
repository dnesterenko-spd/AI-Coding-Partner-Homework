package com.support.util.parser;

import com.support.dto.CreateTicketRequest;

import java.io.InputStream;
import java.util.List;

public interface FileParser {

    List<CreateTicketRequest> parse(InputStream inputStream) throws Exception;

    String getSupportedFormat();
}