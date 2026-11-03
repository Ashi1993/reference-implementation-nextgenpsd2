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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.AbstractConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.AccountInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.DetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.EnrichConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredBasicConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.AccountConsentUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentInitiationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentPathUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.HeaderUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.JsonUtil;

import javax.ws.rs.core.Response;

/**
 * Consent handler for account consents.
 */
public class AccountConsentInitiationHandler extends AbstractConsentInitiationHandler {

    private static final Log log = LogFactory.getLog(AccountConsentInitiationHandler.class);

    /**
     * An account consent additionally requires the PSU-IP-Address header, since the accounts of the PSU are
     * read under it.
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
        CommonConsentValidationUtil.validateJSONFromModel(requestPayload.toString(),
                AccountInitiationPayload.class);
    }

    @Override
    protected String getConsentType(PreProcessConsentCreationRequestBody requestBody) {
        return ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString();
    }

    @Override
    protected String getInitialConsentStatus() {
        return ExtensionEnums.ConsentStatusEnum.RECEIVED.toString();
    }

    @Override
    protected String getRetrievedConsentType(PreProcessConsentRequestBody requestBody) {
        return ConsentPathUtil.getConsentTypeFromRequestPath(requestBody.getData().getConsentResourcePath());
    }

    /**
     * An account consent carries the validity the TPP requested, so that a recurring consent expires with it.
     */
    @Override
    protected void setConsentValidity(DetailedConsentResourceData consentResource, JSONObject requestPayload)
            throws ValidationFailureException, ExtensionException {
        AccountInitiationPayload payload = readValidatedPayload(requestPayload);

        boolean recurringIndicator = payload.getRecurringIndicator();
        consentResource.setRecurringIndicator(recurringIndicator);
        consentResource.setFrequency(payload.getFrequencyPerDay());

        // A one off consent carries no validity period of its own
        consentResource.setValidityTime(recurringIndicator
                ? AccountConsentUtil.convertToUtcTimestamp(String.valueOf(payload.getValidUntil())) : 0L);
    }

    /**
     * Reads the initiation payload of the consent being created, which has already been validated against the
     * model by {@link #validateInitiationPayload(JSONObject, PreProcessConsentCreationRequestBody)}.
     *
     * @param requestPayload initiation payload of the consent being created
     * @return the payload read as an account initiation payload
     * @throws ExtensionException if the validated payload cannot be read, which cannot happen
     */
    private AccountInitiationPayload readValidatedPayload(JSONObject requestPayload) throws ExtensionException {
        try {
            return JsonUtil.readValue(requestPayload.toString(), AccountInitiationPayload.class);
        } catch (JsonProcessingException e) {
            throw new ExtensionException(Response.Status.INTERNAL_SERVER_ERROR, ErrorConstants.SERVER_ERROR,
                    ErrorConstants.PAYLOAD_FORMAT_ERROR, e);
        }
    }

    /**
     * An expired account consent is reported as expired, and its retrieval returns the consent receipt along
     * with the attributes the specification defines for an account consent.
     */
    @Override
    protected void validateRetrieval(String requestId, PreProcessConsentRequestBody requestBody) {
        StoredBasicConsentResourceData consentResource = requestBody.getData().getConsentResource();

        if (consentResource.getRecurringIndicator()
                && AccountConsentUtil.isConsentExpired(consentResource.getValidityTime())
                && !StringUtils.equals(consentResource.getStatus(),
                        ExtensionEnums.ConsentStatusEnum.TERMINATED_BY_TPP.toString())
                && !StringUtils.equals(consentResource.getStatus(),
                        ExtensionEnums.ConsentStatusEnum.REVOKED_BY_PSU.toString())) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("[%s] The Consent is expired", requestId));
            }
            consentResource.setStatus(ExtensionEnums.ConsentStatusEnum.EXPIRED.toString());
        }
    }

    @Override
    protected JSONObject buildRetrievalPayload(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        StoredBasicConsentResourceData consentResource = requestBody.getData().getConsentResource();

        if (StringUtils.contains(requestBody.getData().getConsentResourcePath(), CommonConstants.STATUS)) {
            return super.buildRetrievalPayload(requestBody);
        }

        JSONObject payloadToSend = JsonUtil.convertObjectToJson(consentResource.getReceipt());
        AccountConsentUtil.extendAccountConsentGetResponse(consentResource, payloadToSend);

        return payloadToSend;
    }

    /**
     * Handles account consent creation response customization. Upon a successful consent creation, this method is
     * responsible for building the specification specified consent creation response.
     *
     * @param requestBody body of the request received by enrich-consent-creation-response endpoint
     * @return Response to forward
     */
    @Override
    public SuccessResponseForResponseAlternation enrichCreationResponse(EnrichConsentCreationRequestBody requestBody)
            throws ExtensionException {
        return ConsentInitiationUtil.buildResponseAlterationResponseForConsentCreation(requestBody,
                ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString());
    }
}
