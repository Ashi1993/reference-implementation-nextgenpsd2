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

import org.wso2.openbanking.nextgenpsd2.extensions.model.FundsConfirmationInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;

import java.time.LocalDate;
import java.time.ZoneOffset;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator implementation for validating funds confirmation consent initiation payload.
 */
public class FundsConfirmationInitiationPayloadValidator implements
        ConstraintValidator<ValidFundsConfirmationInitiationPayload, FundsConfirmationInitiationPayload> {

    @Override
    public boolean isValid(FundsConfirmationInitiationPayload payload,
                           ConstraintValidatorContext context) {
        if (payload == null) {
            return false;
        }

        context.disableDefaultConstraintViolation();

        // If card expiry date is present, validate that it's in the future
        if (payload.getCardExpiryDate() != null) {
            if (payload.getCardExpiryDate().isBefore(LocalDate.now(ZoneOffset.UTC))) {
                CommonConsentValidationUtil.setConstrainViolation(context,
                        CommonConsentValidationUtil.buildViolationMessage(
                                TPPMessage.CodeEnum.TIMESTAMP_INVALID,
                                String.format("The provided card expiry date %s is a past date",
                                        payload.getCardExpiryDate())
                        ));
                return false;
            }
        }

        return true;
    }
}
