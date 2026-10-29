/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 * <p>
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *     http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.openbanking.nextgenpsd2.extensions.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Payment instruction including mandatory payment initiation payload attributes.
 */
public class PaymentInstruction {

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.INSTRUCTED_AMOUNT_MISSING)
    @Valid
    private Amount instructedAmount;

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.CREDITOR_ACCOUNT_MISSING)
    @Valid
    private AccountReference creditorAccount;

    @NotBlank(message = "FORMAT_ERROR:" + ErrorConstants.CREDITOR_NAME_MISSING)
    @Size(max = CommonConstants.MAX_CREDITOR_NAME_LENGTH,
            message = "FORMAT_ERROR:" + ErrorConstants.CREDITOR_NAME_INVALID)
    private String creditorName;

    @Pattern(regexp = CommonConstants.BICFI_PATTERN,
            message = "FORMAT_ERROR:" + ErrorConstants.CREDITOR_AGENT_INVALID)
    private String creditorAgent;

    @Size(max = CommonConstants.MAX_CREDITOR_AGENT_NAME_LENGTH,
            message = "FORMAT_ERROR:" + ErrorConstants.CREDITOR_AGENT_NAME_INVALID)
    private String creditorAgentName;

    @Valid
    private Address creditorAddress;

    @Size(max = CommonConstants.MAX_END_TO_END_IDENTIFICATION_LENGTH,
            message = "FORMAT_ERROR:" + ErrorConstants.END_TO_END_IDENTIFICATION_INVALID)
    private String endToEndIdentification;

    @Size(max = CommonConstants.MAX_REMITTANCE_INFO_UNSTRUCTURED_LENGTH,
            message = "FORMAT_ERROR:" + ErrorConstants.REMITTANCE_INFO_UNSTRUCTURED_INVALID)
    private String remittanceInformationUnstructured;

    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    public String getCreditorAgent() {
        return creditorAgent;
    }

    public void setCreditorAgent(String creditorAgent) {
        this.creditorAgent = creditorAgent;
    }

    public String getCreditorAgentName() {
        return creditorAgentName;
    }

    public void setCreditorAgentName(String creditorAgentName) {
        this.creditorAgentName = creditorAgentName;
    }

    public Address getCreditorAddress() {
        return creditorAddress;
    }

    public void setCreditorAddress(Address creditorAddress) {
        this.creditorAddress = creditorAddress;
    }

    public String getEndToEndIdentification() {
        return endToEndIdentification;
    }

    public void setEndToEndIdentification(String endToEndIdentification) {
        this.endToEndIdentification = endToEndIdentification;
    }

    public String getRemittanceInformationUnstructured() {
        return remittanceInformationUnstructured;
    }

    public void setRemittanceInformationUnstructured(String remittanceInformationUnstructured) {
        this.remittanceInformationUnstructured = remittanceInformationUnstructured;
    }

    public Amount getInstructedAmount() {
        return instructedAmount;
    }

    public void setInstructedAmount(Amount instructedAmount) {
        this.instructedAmount = instructedAmount;
    }

    public AccountReference getCreditorAccount() {
        return creditorAccount;
    }

    public void setCreditorAccount(AccountReference creditorAccount) {
        this.creditorAccount = creditorAccount;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public void setCreditorName(String creditorName) {
        this.creditorName = creditorName;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }
}
