package org.wso2.openbanking.nextgenpsd2.extensions.validator.consent;

import org.wso2.openbanking.nextgenpsd2.extensions.model.AccountAccess;
import org.wso2.openbanking.nextgenpsd2.extensions.model.AccountReference;
import org.wso2.openbanking.nextgenpsd2.extensions.model.AdditionalInformationAccess;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                access.getAvailableAccountsWithBalance() != null ||
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
                if (access.getAvailableAccountsWithBalance() != null || access.getAllPsd2() != null) {
                    CommonConsentValidationUtil.setConstrainViolation(context,
                            CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.INVALID_PERMISSION));
                    return false;
                }
            }
            if (access.getAvailableAccountsWithBalance() != null) {
                if (access.getAvailableAccounts() != null || access.getAllPsd2() != null) {
                    CommonConsentValidationUtil.setConstrainViolation(context,
                            CommonConsentValidationUtil.buildViolationMessage(ErrorConstants.INVALID_PERMISSION));
                    return false;
                }
            }
            if (access.getAllPsd2() != null) {
                if (access.getAvailableAccounts() != null || access.getAvailableAccountsWithBalance() != null) {
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

        return isAdditionalInformationConsistent(access, nonEmptyCount > 0, context);
    }

    /**
     * Validates the additional information access against the accounts addressed by the major access attributes.
     *
     * <p>The specification requires that any account addressed within the "additionalInformation" attribute is also
     * addressed by at least one of "accounts", "balances" or "transactions" whenever detailed accounts are
     * referenced. When the major arrays are empty, the additional information arrays have to be empty as well.</p>
     *
     * @param access               account access object sent with the initiation request
     * @param hasReferencedAccount whether the major access attributes reference any detailed account
     * @param context              constraint validator context to report violations on
     * @return true if the additional information access is consistent, false otherwise
     */
    private static boolean isAdditionalInformationConsistent(AccountAccess access, boolean hasReferencedAccount,
                                                             ConstraintValidatorContext context) {

        AdditionalInformationAccess additionalInformation = access.getAdditionalInformation();
        if (additionalInformation == null) {
            return true;
        }

        List<AccountReference> additionalReferences = Stream
                .of(additionalInformation.getOwnerName(), additionalInformation.getTrustedBeneficiaries())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (additionalReferences.isEmpty()) {
            return true;
        }

        // Detailed accounts are not referenced, so the additional information arrays have to be empty as well
        if (!hasReferencedAccount) {
            CommonConsentValidationUtil.setConstrainViolation(context,
                    CommonConsentValidationUtil
                            .buildViolationMessage(ErrorConstants.ADDITIONAL_INFO_ARRAY_NOT_EMPTY));
            return false;
        }

        List<Map<String, String>> addressedAccounts = Stream
                .of(access.getAccounts(), access.getBalances(), access.getTransactions())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(AccountReference::getAdditionalProperties)
                .collect(Collectors.toList());

        for (AccountReference reference : additionalReferences) {
            if (!addressedAccounts.contains(reference.getAdditionalProperties())) {
                CommonConsentValidationUtil.setConstrainViolation(context,
                        CommonConsentValidationUtil
                                .buildViolationMessage(ErrorConstants.ADDITIONAL_INFO_ACCOUNT_NOT_ADDRESSED));
                return false;
            }
        }

        return true;
    }
}
