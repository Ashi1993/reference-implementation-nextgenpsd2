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

import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.validator.consent.ValidPeriodicPaymentInitiationPayload;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Periodic payment initiation payload object used for initiating periodic payment consents.
 */
@ValidPeriodicPaymentInitiationPayload
public class PeriodicPaymentInitiationPayload extends PaymentInstruction {

    @NotNull(message = "FORMAT_ERROR:" + "Debtor account is missing in initiation payload")
    @Valid
    private AccountReference debtorAccount;

    @NotNull(message = "FORMAT_ERROR:" + ErrorConstants.START_DATE_MISSING)
    private LocalDate startDate;

    private LocalDate endDate;

    @NotEmpty(message = "FORMAT_ERROR:" + ErrorConstants.FREQUENCY_MISSING)
    private String frequency;

    private String executionRule;

    @Min(value = 1, message = "FORMAT_ERROR:" + ErrorConstants.INVALID_DAY_OF_EXECUTION)
    @Max(value = 31, message = "FORMAT_ERROR:" + ErrorConstants.INVALID_DAY_OF_EXECUTION)
    private Integer dayOfExecution;

    public AccountReference getDebtorAccount() {
        return debtorAccount;
    }

    public void setDebtorAccount(AccountReference debtorAccount) {
        this.debtorAccount = debtorAccount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getExecutionRule() {
        return executionRule;
    }

    public void setExecutionRule(String executionRule) {
        this.executionRule = executionRule;
    }

    public Integer getDayOfExecution() {
        return dayOfExecution;
    }

    public void setDayOfExecution(Integer dayOfExecution) {
        this.dayOfExecution = dayOfExecution;
    }
}
