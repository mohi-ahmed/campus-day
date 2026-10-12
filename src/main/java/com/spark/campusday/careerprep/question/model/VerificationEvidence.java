package com.spark.campusday.careerprep.question.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationEvidence {
    private String sourceId;
    private String sectionId;
    private String locator;
    private String supportedOptionKey;
    private String note;
}
