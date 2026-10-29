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

package org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ConfigurationConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ConsentAccessContext;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredResource;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.ValidateConsentAccessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;

import java.util.List;

/**
 * Validates funds confirmation requests against a funds confirmation consent.
 */
public class FundsConfirmationConsentValidationHandler implements ConsentValidationHandler {

    private static final Log log = LogFactory.getLog(FundsConfirmationConsentValidationHandler.class);

    @Override
    public void validateConsentAccess(ValidateConsentAccessRequestBody requestBody,
                                      ConsentAccessContext context)
            throws ValidationFailureException, ExtensionException {

        String requestId = requestBody.getRequestId();
        String consentId = requestBody.getData().getConsentId();
        StoredDetailedConsentResourceData consentResource = requestBody.getData().getConsentResource();

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Checking if consent of Id %s is in a valid state", requestId, consentId));
        }
        if (!StringUtils.equals(consentResource.getStatus(),
                ExtensionEnums.ConsentStatusEnum.VALID.toString())) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_UNKNOWN, ErrorConstants.CONSENT_INVALID_STATE));
        }

        validatePayload(requestId, requestBody.getData().getDataRequestPayload(), consentResource);
    }

    /**
     * Validates the funds confirmation payload and the account it addresses.
     *
     * @param requestId          correlation identifier of the request
     * @param dataRequestPayload payload of the funds confirmation request
     * @param consentResource    the stored consent resource
     * @throws ValidationFailureException if the payload is invalid or addresses an unconsented account
     * @throws ExtensionException if the payload could not be read
     */
    private void validatePayload(String requestId, Object dataRequestPayload,
                                 StoredDetailedConsentResourceData consentResource)
            throws ValidationFailureException, ExtensionException {

        if (dataRequestPayload == null) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.PAYLOAD_NOT_PRESENT_ERROR));
        }

        JSONObject payload = CommonConsentValidationUtil.convertObjectToJson(dataRequestPayload);
        if (!payload.has(CommonConstants.ACCOUNT) || !payload.has(CommonConstants.INSTRUCTED_AMOUNT)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.MANDATORY_ELEMENTS_MISSING));
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating the addressed account against the consent", requestId));
        }
        if (!isAddressedAccountConsented(payload.getJSONObject(CommonConstants.ACCOUNT), consentResource)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_INVALID, ErrorConstants.NO_VALID_ACCOUNTS_FOR_CONSENT));
        }
    }

    /**
     * Checks whether the account addressed by the request is bound to the consent. A funds confirmation consent
     * carries a single account, so only the first resource is considered when account ID validation is enabled.
     *
     * @param accountRefObject account reference addressed by the request
     * @param consentResource  the stored consent resource
     * @return true if the addressed account is consented
     */
    private boolean isAddressedAccountConsented(JSONObject accountRefObject,
                                                StoredDetailedConsentResourceData consentResource) {

        List<StoredResource> resources = ConsentValidationUtil.getStoredResources(consentResource);
        if (!ConsentValidationUtil.hasAnyActiveMappingResource(resources)) {
            return false;
        }
        if (!ConfigurationConstants.COF_ACCOUNT_ID_VALIDATION_ENABLED) {
            return true;
        }
        return ConsentValidationUtil.isAccountReferenceMapped(accountRefObject, resources.get(0));
    }
}
