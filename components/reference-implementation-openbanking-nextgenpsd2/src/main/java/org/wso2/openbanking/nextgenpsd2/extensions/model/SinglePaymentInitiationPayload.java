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

import org.wso2.openbanking.nextgenpsd2.extensions.validator.consent.ValidSinglePaymentInitiationPayload;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Single payment initiation payload object used for initiating single payment consents.
 */
@ValidSinglePaymentInitiationPayload
public class SinglePaymentInitiationPayload extends PaymentInstruction {

    @NotNull(message = "FORMAT_ERROR:" + "Debtor account is missing in initiation payload")
    @Valid
    private AccountReference debtorAccount;

    private LocalDate requestedExecutionDate;

    public LocalDate getRequestedExecutionDate() {
        return requestedExecutionDate;
    }

    public void setRequestedExecutionDate(LocalDate requestedExecutionDate) {
        this.requestedExecutionDate = requestedExecutionDate;
    }

    public AccountReference getDebtorAccount() {
        return debtorAccount;
    }

    public void setDebtorAccount(AccountReference debtorAccount) {
        this.debtorAccount = debtorAccount;
    }
}
