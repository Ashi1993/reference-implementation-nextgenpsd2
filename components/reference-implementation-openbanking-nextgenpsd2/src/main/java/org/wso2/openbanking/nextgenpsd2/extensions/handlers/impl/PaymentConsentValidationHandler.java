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
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ConsentAccessContext;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.ValidateConsentAccessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.HttpMethod;

/**
 * Validates data access requests against a payment consent.
 */
public class PaymentConsentValidationHandler implements ConsentValidationHandler {

    private static final Log log = LogFactory.getLog(PaymentConsentValidationHandler.class);

    /**
     * Statuses in which a payment can neither be retrieved nor cancelled.
     */
    private static final List<String> NON_ACCESSIBLE_STATUSES = Arrays.asList(
            ExtensionEnums.TransactionStatusEnum.CANC.name(),
            ExtensionEnums.TransactionStatusEnum.REVOKED.name(),
            ExtensionEnums.TransactionStatusEnum.RJCT.name());

    @Override
    public void validateConsentAccess(ValidateConsentAccessRequestBody requestBody, ConsentAccessContext context)
            throws ValidationFailureException, ExtensionException {

        String requestId = requestBody.getRequestId();
        StoredDetailedConsentResourceData consentResource = requestBody.getData().getConsentResource();
        String currentStatus = consentResource.getStatus();

        if (context.hasResourcePath()) {
            validateAddressedPayment(requestId, context.getResourcePath(), consentResource);
        }

        // A single payment cannot be cancelled, only bulk and periodic payments can
        if (StringUtils.equals(HttpMethod.DELETE, context.getHttpMethod())
                && StringUtils.equals(ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString(),
                consentResource.getType())) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.METHOD_NOT_ALLOWED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CANCELLATION_INVALID, ErrorConstants.CANCELLATION_NOT_APPLICABLE));
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Checking if payment consent is in an accessible state", requestId));
        }
        if (NON_ACCESSIBLE_STATUSES.contains(currentStatus)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_INVALID, ErrorConstants.CONSENT_ALREADY_DELETED));
        }

        /*
         * A payment that is still in RCVD has not been authorised, so it cannot be retrieved. It remains a valid
         * target for a cancellation request, which is why this is checked only when the HTTP method is known to
         * be something other than DELETE.
         */
        if (context.hasHttpMethod() && !StringUtils.equals(HttpMethod.DELETE, context.getHttpMethod())
                && StringUtils.equals(ExtensionEnums.TransactionStatusEnum.RCVD.name(), currentStatus)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_UNKNOWN, ErrorConstants.CONSENT_INVALID_STATE));
        }
    }

    /**
     * Validates that the payment addressed by the request path is the consent being validated, and that the
     * payment service on the path is the one the payment was initiated under.
     *
     * @param requestId       correlation identifier of the request
     * @param resourcePath    path of the resource being accessed
     * @param consentResource the stored consent resource
     * @throws ValidationFailureException if the path addresses a different payment or service
     * @throws ExtensionException if construction of an error failed
     */
    private void validateAddressedPayment(String requestId, String resourcePath,
                                          StoredDetailedConsentResourceData consentResource)
            throws ValidationFailureException, ExtensionException {

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating the payment addressed by the request path", requestId));
        }

        ConsentValidationUtil.validateResourcePathAgainstConsentType(consentResource.getType(), resourcePath);

        String pathPaymentId = ConsentValidationUtil.getPaymentIdFromPath(resourcePath);
        if (!StringUtils.equals(pathPaymentId, consentResource.getId())) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.FORBIDDEN,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_UNKNOWN_403,
                            ErrorConstants.PAYMENT_ID_CONSENT_ID_MISMATCH));
        }
    }
}
