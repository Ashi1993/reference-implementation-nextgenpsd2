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

import org.wso2.openbanking.nextgenpsd2.extensions.model.BulkPaymentInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.PaymentInstruction;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator implementation for validating single payment consent initiation payload.
 */
public class BulkPaymentInitiationPayloadValidator implements
        ConstraintValidator<ValidBulkPaymentInitiationPayload, BulkPaymentInitiationPayload> {

    @Override
    public boolean isValid(BulkPaymentInitiationPayload payload, ConstraintValidatorContext context) {

        if (payload == null) {
            return false;
        }

        // Cannot have both date and time
        if (payload.getRequestedExecutionDate() != null && payload.getRequestedExecutionTime() != null) {
            CommonConsentValidationUtil.setConstrainViolation(context,
                    CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.EXECUTION_DATE_TIME_ERROR));
            return false;
        }

        // Bulk entries must not carry the elements that only belong to the enclosing bulk payment
        if (payload.getPayments() != null) {
            for (PaymentInstruction entry : payload.getPayments()) {
                if (entry == null) {
                    continue;
                }
                for (String forbiddenElement : CommonConstants.FORBIDDEN_BULK_ENTRY_ELEMENTS) {
                    if (entry.getAdditionalProperties().containsKey(forbiddenElement)) {
                        CommonConsentValidationUtil.setConstrainViolation(context, CommonConsentValidationUtil
                                .buildViolationMessage(ErrorConstants.FORBIDDEN_BULK_ENTRY_ELEMENT));
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
