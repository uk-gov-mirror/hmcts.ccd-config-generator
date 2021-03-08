package uk.gov.hmcts.ccd.sdk.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

@Data
@ComplexType(name = "Address", generate = false)
public class Address {

  @JsonProperty("AddressLine1")
  private final String addressLine1;

  @JsonProperty("AddressLine2")
  private final String addressLine2;

  @JsonProperty("AddressLine3")
  private final String addressLine3;

  @JsonProperty("PostTown")
  private final String postTown;

  @JsonProperty("County")
  private final String county;

  @JsonProperty("PostCode")
  private final String postCode;

  @JsonProperty("Country")
  private final String country;

}
