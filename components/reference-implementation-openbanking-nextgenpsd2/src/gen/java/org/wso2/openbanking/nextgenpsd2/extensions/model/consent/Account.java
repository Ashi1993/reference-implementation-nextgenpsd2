package org.wso2.openbanking.nextgenpsd2.extensions.model.consent;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.*;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * A user account or resource representation
 **/
@ApiModel(description = "A user account or resource representation")
@JsonTypeName("Account")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2025-07-14T12:25:16.039700400+05:30[Asia/Colombo]", comments = "Generator version: 7.12.0")
public class Account  {
  private String displayName;
  private Map<String, Object> additionalProperties = new HashMap<>();

  public Account() {
  }

  @JsonCreator
  public Account(
    @JsonProperty(required = true, value = "displayName") String displayName
  ) {
    this.displayName = displayName;
  }

  /**
   * Account display name
   **/
  public Account displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Account display name")
  @JsonProperty(required = true, value = "displayName")
  @NotNull public String getDisplayName() {
    return displayName;
  }

  @JsonProperty(required = true, value = "displayName")
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Account account = (Account) o;
    return Objects.equals(this.displayName, account.displayName) &&
        Objects.equals(this.additionalProperties, account.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, additionalProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Account {\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }


}

