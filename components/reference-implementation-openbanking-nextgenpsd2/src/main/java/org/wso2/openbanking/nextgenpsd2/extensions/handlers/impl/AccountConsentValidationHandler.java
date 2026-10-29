/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ConfigurationConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ConsentAccessContext;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredResource;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.ValidateConsentAccessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.AccountConsentUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;

import java.util.List;

/**
 * Validates data access requests against an account consent.
 */
public class AccountConsentValidationHandler implements ConsentValidationHandler {

    private static final Log log = LogFactory.getLog(AccountConsentValidationHandler.class);

    @Override
    public void validateConsentAccess(ValidateConsentAccessRequestBody requestBody, ConsentAccessContext context)
            throws ValidationFailureException, ExtensionException {

        String requestId = requestBody.getRequestId();
        String consentId = requestBody.getData().getConsentId();
        StoredDetailedConsentResourceData consentResource = requestBody.getData().getConsentResource();

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Checking if consent of Id %s is expired", requestId, consentId));
        }
        if (isConsentExpired(consentResource)) {
            // The accelerator owns the status transition to expired, either through the periodical expiration
            // job or through a scheduled consent expiry. The extension only denies access.
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_EXPIRED, ErrorConstants.CONSENT_EXPIRED));
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Checking if consent of Id %s is in a valid state", requestId, consentId));
        }
        if (!StringUtils.equals(consentResource.getStatus(),
                ExtensionEnums.ConsentStatusEnum.VALID.toString())) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_UNKNOWN, ErrorConstants.CONSENT_INVALID_STATE));
        }

        List<StoredResource> resources = ConsentValidationUtil.getStoredResources(consentResource);
        if (!ConsentValidationUtil.hasAnyActiveMappingResource(resources)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_INVALID, ErrorConstants.NO_VALID_ACCOUNTS_FOR_CONSENT));
        }

        if (context.hasResourcePath()
                && ConsentValidationUtil.isSingleAccountRetrieveRequest(context.getResourcePath())) {
            validateSingleAccountAccess(requestId, context.getResourcePath(), resources);
        }
    }

    /**
     * Validates that the account addressed by a single account request carries the permission the request needs.
     * A card account is always validated against the consent rather than the account ID, since a card account
     * reference is validated by the bank.
     *
     * @param requestId    correlation identifier of the request
     * @param resourcePath path of the resource being accessed
     * @param resources    resources mapped to the consent
     * @throws ValidationFailureException if the account does not carry the required permission
     * @throws ExtensionException if construction of an error failed
     */
    private void validateSingleAccountAccess(String requestId, String resourcePath, List<StoredResource> resources)
            throws ValidationFailureException, ExtensionException {

        List<String> pathElements = ConsentValidationUtil.splitPath(resourcePath);
        String accountId = ConsentValidationUtil.getAccountIdFromPath(pathElements);
        String accessMethod = ConsentValidationUtil.getAccessMethodFromPath(pathElements);

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating %s access to a single account", requestId, accessMethod));
        }

        if (StringUtils.isBlank(accountId)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_INVALID, ErrorConstants.ACCOUNT_ID_CANNOT_BE_EMPTY));
        }

        boolean isCardAccountRequest = pathElements.contains(CommonConstants.CARD_ACCOUNTS);
        if (ConfigurationConstants.ACCOUNT_ID_VALIDATION_ENABLED && !isCardAccountRequest) {
            if (!ConsentValidationUtil.hasValidPermissionsForAccountId(accountId, accessMethod, resources)) {
                throw new ValidationFailureException(ValidationFailureException.ErrorCode.NOT_FOUND,
                        ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                                TPPMessage.CodeEnum.RESOURCE_UNKNOWN_404,
                                ErrorConstants.NO_MATCHING_PERMISSIONS_FOR_ACCOUNT_ID));
            }
        } else {
            if (!ConsentValidationUtil.hasValidPermissionsForConsentId(accessMethod, resources)) {
                throw new ValidationFailureException(ValidationFailureException.ErrorCode.UNAUTHORIZED,
                        ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                                TPPMessage.CodeEnum.CONSENT_INVALID,
                                ErrorConstants.NO_MATCHING_PERMISSIONS_FOR_ACCOUNT_ID));
            }
        }
    }

    /**
     * Checks whether the consent is expired, either by its status or by its validity period elapsing. Only
     * recurring consents carry a validity period.
     *
     * @param consentResource the stored consent resource
     * @return true if the consent is expired
     */
    private boolean isConsentExpired(StoredDetailedConsentResourceData consentResource) {

        if (StringUtils.equals(consentResource.getStatus(),
                ExtensionEnums.ConsentStatusEnum.EXPIRED.toString())) {
            return true;
        }
        return BooleanUtils.isTrue(consentResource.getRecurringIndicator())
                && consentResource.getValidityTime() != null
                && AccountConsentUtil.isConsentExpired(consentResource.getValidityTime());
    }
}
