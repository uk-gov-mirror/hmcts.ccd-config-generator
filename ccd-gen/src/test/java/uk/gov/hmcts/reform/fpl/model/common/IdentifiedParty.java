package uk.gov.hmcts.reform.fpl.model.common;

import ccd.sdk.types.CaseField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.hmcts.reform.fpl.enums.PartyType;
import uk.gov.hmcts.reform.fpl.model.Address;

import java.time.LocalDate;

@Data
public class IdentifiedParty extends Party {
    @CaseField(label = "First name")
    public final String firstName;
    @CaseField(label = "Last name")
    public final String lastName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @CaseField(label = "Date of birth", hint = "For example, 31 3 1980")
    public final LocalDate dateOfBirth;

    public IdentifiedParty(String partyId, PartyType partyType, Address address, String firstName, String lastName, LocalDate dateOfBirth) {
        super(partyId, partyType, address);
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }
}
