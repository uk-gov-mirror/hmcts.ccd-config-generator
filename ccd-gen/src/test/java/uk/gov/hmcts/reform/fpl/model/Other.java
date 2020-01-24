package uk.gov.hmcts.reform.fpl.model;

import uk.gov.hmcts.ccd.sdk.types.CaseField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Other {
    @SuppressWarnings("membername")
    private final String DOB;
    private final String name;
    private final String gender;
    private final Address address;
    private final String telephone;
    @CaseField(ignore = true)
    private final String birthplace;
    private final String childInformation;
    private final String litigationIssues;
    private final String genderIdentification;
}
