package com.support.dto;

import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {

    @Size(min = 1, max = 200, message = "Subject must be between 1 and 200 characters")
    private String subject;

    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    private Category category;
    private Priority priority;
    private Status status;
    private String assignedTo;
    private Set<String> tags;
}