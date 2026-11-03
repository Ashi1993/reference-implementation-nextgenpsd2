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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.AbstractConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.EnrichConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentInitiationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentPathUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.HeaderUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.PaymentConsentUtil;

import java.util.Map;

/**
 * Consent handler for payment consents.
 */
public class PaymentConsentInitiationHandler extends AbstractConsentInitiationHandler {

    private static final Log log = LogFactory.getLog(PaymentConsentInitiationHandler.class);


    /**
     * A payment consent additionally requires the PSU-IP-Address header, since the payment is initiated on
     * behalf of the PSU.
     */
    @Override
    protected void validateCreationHeaders(String requestId, JSONObject headers)
            throws ValidationFailureException, ExtensionException {
        super.validateCreationHeaders(requestId, headers);
        HeaderUtil.validatePsuIpAddress(requestId, headers);
    }

    @Override
    protected void validateInitiationPayload(JSONObject requestPayload,
                                             PreProcessConsentCreationRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        PaymentConsentUtil.validatePaymentInitiationPayload(requestPayload,
                requestBody.getData().getConsentResourcePath());
    }

    @Override
    protected String getConsentType(PreProcessConsentCreationRequestBody requestBody) {
        return ConsentPathUtil.getConsentTypeFromRequestPath(requestBody.getData().getConsentResourcePath());
    }

    @Override
    protected String getInitialConsentStatus() {
        return ExtensionEnums.TransactionStatusEnum.RCVD.name();
    }

    @Override
    protected String getRetrievedConsentType(PreProcessConsentRequestBody requestBody) {
        return ConsentPathUtil.getConsentTypeFromRequestPath(requestBody.getData().getConsentResourcePath());
    }

    /**
     * A payment consent additionally stores the payment product it was initiated under, so that a retrieval
     * made under another product can be rejected.
     */
    @Override
    protected Map<String, Object> getConsentAttributes(PreProcessConsentCreationRequestBody requestBody,
                                                       JSONObject headers)
            throws ValidationFailureException, ExtensionException {
        return Map.of(
                CommonConstants.X_REQUEST_ID_HEADER, headers.getString(CommonConstants.X_REQUEST_ID_HEADER),
                CommonConstants.PAYMENT_PRODUCT_CC,
                PaymentConsentUtil.getPaymentProduct(requestBody.getData().getConsentResourcePath())
        );
    }

    /**
     * The payment is addressed on the path of the payment product it was initiated under.
     */
    @Override
    protected void validateRetrieval(String requestId, PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating consent of Id %s for correct payment product", requestId,
                    requestBody.getData().getConsentId()));
        }
        PaymentConsentUtil.validatePaymentProductFromAttributes(
                requestBody.getData().getConsentResource().getAttributes(),
                requestBody.getData().getConsentResourcePath());
    }

    /*
     * Revocation is handled as the base handler defines it. A single payment consent cannot be revoked once
     * created, which is rejected while the revocation request is validated.
     * ToDo: Implement cancel-authorizations once explicit auth is implemented
     */

    /**
     * Handles payment consent creation response customization. Upon a successful consent creation, this method is
     * responsible for building the specification specified consent creation response.
     *
     * @param requestBody body of the request received by enrich-consent-creation-response endpoint
     * @return Response to forward
     */
    @Override
    public SuccessResponseForResponseAlternation enrichCreationResponse(EnrichConsentCreationRequestBody requestBody)
            throws ExtensionException {
        return ConsentInitiationUtil.buildResponseAlterationResponseForConsentCreation(requestBody,
                ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString());
    }
}
