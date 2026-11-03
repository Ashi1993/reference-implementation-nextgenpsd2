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

import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.AbstractConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.FundsConfirmationInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.EnrichConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentInitiationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;

/**
 * Consent handler for funds confirmation consents.
 */
public class FundsConfirmationConsentInitiationHandler extends AbstractConsentInitiationHandler {

    @Override
    protected void validateInitiationPayload(JSONObject requestPayload,
                                             PreProcessConsentCreationRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        CommonConsentValidationUtil.validateJSONFromModel(requestPayload.toString(),
                FundsConfirmationInitiationPayload.class);
    }

    @Override
    protected String getConsentType(PreProcessConsentCreationRequestBody requestBody) {
        return ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString();
    }

    @Override
    protected String getInitialConsentStatus() {
        return ExtensionEnums.ConsentStatusEnum.RECEIVED.toString();
    }

    @Override
    protected String getRetrievedConsentType(PreProcessConsentRequestBody requestBody) {
        return ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString();
    }

    /**
     * Handles CoF consent creation response customization. Upon a successful consent creation, this method is
     * responsible for building the specification specified consent creation response.
     *
     * @param requestBody body of the request received by enrich-consent-creation-response endpoint
     * @return Response to forward
     */
    @Override
    public SuccessResponseForResponseAlternation enrichCreationResponse(EnrichConsentCreationRequestBody requestBody)
            throws ExtensionException {
        return ConsentInitiationUtil.buildResponseAlterationResponseForConsentCreation(requestBody,
                ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString());
    }
}
