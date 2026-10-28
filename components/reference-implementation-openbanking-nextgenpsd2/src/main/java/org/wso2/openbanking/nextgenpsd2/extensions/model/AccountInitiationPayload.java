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
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.validator.consent.ValidAccountInitiationPayload;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Account initiation payload object used for initiating account consents.
 */
@ValidAccountInitiationPayload
public class AccountInitiationPayload {

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.MANDATORY_ELEMENTS_MISSING)
    @Valid
    private AccountAccess access;

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.MANDATORY_ELEMENTS_MISSING)
    private Boolean recurringIndicator;

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.MANDATORY_ELEMENTS_MISSING)
    @Min(value = 1, message = "FORMAT_ERROR:" + ErrorConstants.INVALID_FREQ_PER_DAY)
    private Integer frequencyPerDay;

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.MANDATORY_ELEMENTS_MISSING)
    private LocalDate validUntil;

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.MANDATORY_ELEMENTS_MISSING)
    @AssertFalse(message = "FORMAT_ERROR:" + ErrorConstants.COMBINED_SERVICE_INDICATOR_NOT_SUPPORTED)
    private Boolean combinedServiceIndicator;

    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    public AccountAccess getAccess() {
        return access;
    }

    public void setAccess(AccountAccess access) {
        this.access = access;
    }

    public Boolean getRecurringIndicator() {
        return recurringIndicator;
    }

    public void setRecurringIndicator(Boolean recurringIndicator) {
        this.recurringIndicator = recurringIndicator;
    }

    public Integer getFrequencyPerDay() {
        return frequencyPerDay;
    }

    public void setFrequencyPerDay(Integer frequencyPerDay) {
        this.frequencyPerDay = frequencyPerDay;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public Boolean getCombinedServiceIndicator() {
        return combinedServiceIndicator;
    }

    public void setCombinedServiceIndicator(Boolean combinedServiceIndicator) {
        this.combinedServiceIndicator = combinedServiceIndicator;
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
