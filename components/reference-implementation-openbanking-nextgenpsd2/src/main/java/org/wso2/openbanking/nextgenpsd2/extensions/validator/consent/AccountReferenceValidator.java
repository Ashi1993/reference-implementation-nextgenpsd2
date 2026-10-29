/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 * <p>
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.openbanking.nextgenpsd2.extensions.validator.consent;

import org.apache.commons.lang3.StringUtils;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ConfigurationConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.model.AccountReference;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator implementation for validating account reference.
 */
public class AccountReferenceValidator implements ConstraintValidator<ValidAccountReference, AccountReference> {

    @Override
    public boolean isValid(AccountReference ref, ConstraintValidatorContext context) {
        Map<String, String> props = ref.getAdditionalProperties();

        if (props == null || props.isEmpty()) {
            CommonConsentValidationUtil.setConstrainViolation(context,
                    CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.ACCOUNT_REFERENCE_OBJECT_MISSING));
            return false;
        }

        Set<String> keys = props.keySet();

        List<String> supportedKeys = ConfigurationConstants.SUPPORTED_ACC_REFERNCE_TYPES;
        if (keys.size() == 1) {
            if (!supportedKeys.contains(keys.iterator().next())) {
                CommonConsentValidationUtil.setConstrainViolation(context,
                        CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.INVALID_ACCOUNT_REFERENCE));
                return false;
            }
        } else if (keys.size() == 2) {
            boolean hasCurrency = keys.contains(CommonConstants.CURRENCY);
            boolean hasRefType = keys.stream().anyMatch(supportedKeys::contains);
            if (!hasCurrency || !hasRefType) {
                CommonConsentValidationUtil.setConstrainViolation(context,
                        CommonConsentValidationUtil
                                .buildViolationMessage(ErrorConstants.UNRECOGNIZED_ATTRIBUTES_ACCOUNT_REFERENCE));
                return false;
            }
        } else {
            CommonConsentValidationUtil.setConstrainViolation(context,
                    CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.ACCOUNT_REFERENCE_OBJECT_MISSING));
            return false;
        }

        // Ensure reference value is not blank
        for (String key : keys) {
            if (!CommonConstants.CURRENCY.equals(key) && StringUtils.isBlank(props.get(key))) {
                CommonConsentValidationUtil.setConstrainViolation(context,
                        CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.ACCOUNT_REFERENCE_IS_EMPTY));
                return false;
            }
        }

        // Ensure each provided element complies with the format defined by the PIS specification
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String formatError = validateElementFormat(entry.getKey(), entry.getValue());
            if (formatError != null) {
                CommonConsentValidationUtil.setConstrainViolation(context,
                        CommonConsentValidationUtil.buildViolationMessage(formatError));
                return false;
            }
        }

        return true;
    }

    /**
     * Validates a single account reference element against the format defined by the PIS specification.
     *
     * @param key   name of the account reference element
     * @param value value of the account reference element
     * @return the error message if the value is in an incorrect format, null otherwise
     */
    private static String validateElementFormat(String key, String value) {
        switch (key) {
            case CommonConstants.IBAN:
                return value.matches(CommonConstants.IBAN_PATTERN) ? null : ErrorConstants.IBAN_INVALID;

            case CommonConstants.BBAN:
                return value.matches(CommonConstants.BBAN_PATTERN) ? null : ErrorConstants.BBAN_INVALID;

            case CommonConstants.PAN:
                return value.length() <= CommonConstants.MAX_PAN_LENGTH ? null : ErrorConstants.PAN_INVALID;

            case CommonConstants.MASKED_PAN:
                return value.length() <= CommonConstants.MAX_MASKED_PAN_LENGTH
                        ? null : ErrorConstants.MASKED_PAN_INVALID;

            case CommonConstants.MSISDN:
                return value.length() <= CommonConstants.MAX_MSISDN_LENGTH ? null : ErrorConstants.MSISDN_INVALID;

            case CommonConstants.CURRENCY:
                return value.matches(CommonConstants.CURRENCY_CODE_PATTERN)
                        ? null : ErrorConstants.ACCOUNT_REFERENCE_CURRENCY_INVALID;

            default:
                return null;
        }
    }
}
