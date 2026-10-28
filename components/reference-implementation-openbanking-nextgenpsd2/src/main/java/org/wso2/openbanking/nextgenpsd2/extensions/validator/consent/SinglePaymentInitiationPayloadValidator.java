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

package org.wso2.openbanking.nextgenpsd2.extensions.validator.consent;

import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ConfigurationConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.model.SinglePaymentInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator implementation for validating single payment consent initiation payload.
 */
public class SinglePaymentInitiationPayloadValidator implements
        ConstraintValidator<ValidSinglePaymentInitiationPayload, SinglePaymentInitiationPayload> {

    private final long maxDays = Long.parseLong(ConfigurationConstants.MAX_FUTURE_PAYMENT_DAYS);

    @Override
    public boolean isValid(SinglePaymentInitiationPayload payload, ConstraintValidatorContext context) {
        if (payload == null) {
            return false;
        }

        // validate requestedExecutionDate if given
        LocalDate requestedExecutionDate = payload.getRequestedExecutionDate();
        if (requestedExecutionDate != null) {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);

            // Checks if execution date is in future
            if (!requestedExecutionDate.isAfter(today)) {
                CommonConsentValidationUtil.setConstrainViolation(context,
                        CommonConsentValidationUtil.buildViolationMessage(TPPMessage.CodeEnum.EXECUTION_DATE_INVALID,
                                ErrorConstants.EXECUTION_DATE_NOT_FUTURE));
                return false;
            }

            // Checks if it exceeds allowed maximum number of days into the future
            if (ChronoUnit.DAYS.between(today.plusDays(maxDays), requestedExecutionDate) > 0) {
                CommonConsentValidationUtil.setConstrainViolation(context,
                        CommonConsentValidationUtil.buildViolationMessage(TPPMessage.CodeEnum.EXECUTION_DATE_INVALID,
                                ErrorConstants.PAYMENT_EXECUTION_DATE_EXCEEDED));
                return false;
            }
        }

        return true;
    }
}
