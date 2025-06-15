package com.naji.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SubmissionDTO {
    private Long id;
    private Long roundId;
    private Long playerId;
    private String text;
    private Boolean processed;
}
