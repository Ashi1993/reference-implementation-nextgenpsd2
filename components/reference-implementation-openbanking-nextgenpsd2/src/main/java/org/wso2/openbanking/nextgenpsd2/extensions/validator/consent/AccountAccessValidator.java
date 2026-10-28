package org.wso2.openbanking.nextgenpsd2.extensions.validator.consent;

import org.wso2.openbanking.nextgenpsd2.extensions.model.AccountAccess;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;

import java.util.Arrays;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator implementation for validating account access object.
 */
public class AccountAccessValidator implements ConstraintValidator<ValidAccountAccess, AccountAccess> {

    @Override
    public boolean isValid(AccountAccess access, ConstraintValidatorContext context) {
        boolean hasArrays = access.getAccounts() != null ||
                access.getTransactions() != null ||
                access.getBalances() != null;

        boolean hasPermissions = access.getAvailableAccounts() != null ||
                access.getAvailableAccountsWithBalances() != null ||
                access.getAllPsd2() != null;

        // At least one must be present
        if (!hasArrays && !hasPermissions) {
            CommonConsentValidationUtil.setConstrainViolation(context,
                    CommonConsentValidationUtil
                            .buildViolationMessage(ErrorConstants.ACCESS_OBJECT_MANDATORY_ELEMENTS_MISSING));
            return false;
        }

        // Permissions and arrays must not coexist
        if (hasArrays && hasPermissions) {
            CommonConsentValidationUtil.setConstrainViolation(context,
                    CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.INVALID_PERMISSION));
            return false;
        }

        // Additional info only with at least one array
        if (access.getAdditionalInformation() != null && !hasArrays) {
            CommonConsentValidationUtil.setConstrainViolation(context,
                    CommonConsentValidationUtil
                            .buildViolationMessage(ErrorConstants.INVALID_USE_OF_ADDITIONAL_INFO_ATTRIBUTE));
            return false;
        }

        // Validate permission configurations
        if (hasPermissions) {
            if (access.getAvailableAccounts() != null) {
                if (access.getAvailableAccountsWithBalances() != null || access.getAllPsd2() != null) {
                    CommonConsentValidationUtil.setConstrainViolation(context,
                            CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.INVALID_PERMISSION));
                    return false;
                }
            }
            if (access.getAvailableAccountsWithBalances() != null) {
                if (access.getAvailableAccounts() != null || access.getAllPsd2() != null) {
                    CommonConsentValidationUtil.setConstrainViolation(context,
                            CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.INVALID_PERMISSION));
                    return false;
                }
            }
            if (access.getAllPsd2() != null) {
                if (access.getAvailableAccounts() != null || access.getAvailableAccountsWithBalances() != null) {
                    CommonConsentValidationUtil.setConstrainViolation(context,
                            CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.INVALID_PERMISSION));
                    return false;
                }
            }

            // Should be unreachable because of a prior schema validation
            return true;
        }

        // Arrays must all be empty or all non-empty
        List<List<?>> arrays = Arrays.asList(access.getAccounts(), access.getBalances(), access.getTransactions());
        long emptyCount = arrays.stream().filter(arr -> arr != null && arr.isEmpty()).count();
        long nonEmptyCount = arrays.stream().filter(arr -> arr != null && !arr.isEmpty()).count();

        if (emptyCount > 0 && nonEmptyCount > 0) {
            CommonConsentValidationUtil.setConstrainViolation(context,
                    CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.INVALID_PERMISSION));
            return false;
        }

        return true;
    }
}
