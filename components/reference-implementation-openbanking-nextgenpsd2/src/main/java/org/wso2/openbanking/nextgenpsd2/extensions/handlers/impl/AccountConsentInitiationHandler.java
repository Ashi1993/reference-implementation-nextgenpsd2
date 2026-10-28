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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ConfigurationConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.AccountInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.Authorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.DetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.EnrichConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRetrievalData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredBasicConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseConsentRevocation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternationData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePreProcessConsentCreation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseWithDetailedConsentData;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.AccountConsentUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentInitiationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

/**
 * Consent handler for account consents.
 */
public class AccountConsentInitiationHandler implements ConsentInitiationHandler {
    private static final Log log = LogFactory.getLog(AccountConsentInitiationHandler.class);

    /**
     * Handles creation of account consents. This method is responsible for completing specification specific
     * validations on the consent initiation payload.
     *
     * @param requestBody body of the request received by pre-process-consent-creation endpoint
     * @return Successful validation result
     */
    @Override
    public SuccessResponsePreProcessConsentCreation handleCreation(PreProcessConsentCreationRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        String requestId = requestBody.getRequestId();

        // Skipping idempotency check as it's handled by the accelerator
        // ToDo: Add explicit authorisation support

        boolean isSCARequired = Boolean.parseBoolean(ConfigurationConstants.IS_SCA_REQUIRED);

        JSONObject headersJSON = CommonConsentValidationUtil.convertObjectToJson(requestBody.getData()
                .getRequestHeaders());

        // Validate headers
        CommonConsentValidationUtil.validateTppRedirectPreferredHeader(requestId, headersJSON);
        CommonConsentValidationUtil.validatePsuIpAddress(requestId, headersJSON);

        // Validate payload
        JSONObject requestPayload;
        try {
            requestPayload = CommonConsentValidationUtil
                    .convertObjectToJson(requestBody.getData().getConsentInitiationData());
        } catch (JSONException e) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("payload", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.PAYLOAD_FORMAT_ERROR));
        }

        // Parse account initiation payload and validate its structure
        AccountInitiationPayload payload = CommonConsentValidationUtil
                .validateJSONFromModel(requestPayload.toString(), AccountInitiationPayload.class);

        Optional<Boolean> isRedirectPreferred = CommonConsentValidationUtil.isTppRedirectPreferred(requestId,
                headersJSON);

        if (!isRedirectPreferred.isPresent() || BooleanUtils.isTrue(isRedirectPreferred.get())) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("[%s] SCA approach is Redirect SCA (OAuth2)", requestId));
            }
            String authStatus = CommonConsentValidationUtil.getAuthorizationStatus(isSCARequired, headersJSON);

            // Response body
            SuccessResponsePreProcessConsentCreation validationResponse =
                    new SuccessResponsePreProcessConsentCreation();
            validationResponse.setResponseId(requestBody.getRequestId());
            validationResponse.setStatus(SuccessResponsePreProcessConsentCreation.StatusEnum.SUCCESS);

            // Response data, contains consentResource
            SuccessResponseWithDetailedConsentData data = new SuccessResponseWithDetailedConsentData();

            // Consent resource
            DetailedConsentResourceData consentResource = new DetailedConsentResourceData();
            consentResource.setReceipt(requestPayload);
            consentResource.setType(ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString());
            consentResource.setStatus(ExtensionEnums.ConsentStatusEnum.RECEIVED.toString());

            // Setting additional properties to consent resource
            boolean recurringIndicator = payload.getRecurringIndicator();
            consentResource.setRecurringIndicator(recurringIndicator);
            consentResource.setFrequency(payload.getFrequencyPerDay());

            String validUntilString = String.valueOf(payload.getValidUntil());
            if (recurringIndicator) {
                consentResource.setValidityTime(AccountConsentUtil.convertToUtcTimestamp(validUntilString));
            } else {
                // setting null for one off consent's validity period
                consentResource.setValidityTime(0L);
            }

            Map<String, Object> attributes = Map.of(
                    CommonConstants.X_REQUEST_ID_HEADER, headersJSON.getString(CommonConstants.X_REQUEST_ID_HEADER)
            );
            consentResource.setAttributes(attributes);

            // Build auth resource for implicit authorisation
            // ToDo: Revisit once explicit authorisation is supported
            Authorization authObj = new Authorization();
            if (headersJSON.has(CommonConstants.PSU_ID_HEADER)) {
                authObj.setUserId(headersJSON.getString(CommonConstants.PSU_ID_HEADER));
            }
            authObj.setType(ExtensionEnums.AuthTypeEnum.AUTHORISATION.toString());
            authObj.setStatus(authStatus);

            // Append auth resource to consent
            consentResource.addAuthorizationsItem(authObj);

            // Envelop consent in response data
            data.setConsentResource(consentResource);

            // Append response data to response
            validationResponse.setData(data);

            return validationResponse;
        } else {
            //ToDo: revisit once decoupled approach is implemented.
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, String.format("%s SCA Approach is not supported",
                                    ExtensionEnums.ScaApproachEnum.DECOUPLED)));
        }
    }

    /**
     * Handles retrieval of account requests. This method is responsible for validating the requested consent and
     * building the response encompassing consent information as specified by the specification.
     *
     * @param requestBody body of the request received by pre-process-consent-retrieval
     * @return Successful retrieval response
     */
    @Override
    public SuccessResponseForResponseAlternation handleRetrieval(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        String requestId = requestBody.getRequestId();

        PreProcessConsentRetrievalData data = requestBody.getData();
        String requestPath = data.getConsentResourcePath();
        String consentType = CommonConsentValidationUtil.getConsentTypeFromRequestPath(requestPath);
        String consentId = data.getConsentId();
        StoredBasicConsentResourceData consentResource = data.getConsentResource();

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating consent of Id %s for valid client", requestId, consentId));
        }

        // Get request client id from the headers
        String requestClientId;
        JSONObject headers;
        try {
            headers = CommonConsentValidationUtil.convertObjectToJson(data.getRequestHeaders());
            requestClientId = headers.getString(CommonConstants.X_WSO2_CLIENT_ID_KEY);
        } catch (JSONException e) {
            throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                    "Organization ID not found for the client");
        }

        // Validate client
        CommonConsentValidationUtil.validateClient(requestClientId, data.getConsentResource().getClientId());

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating consent of Id %s for correct type", requestId, consentId));
        }
        CommonConsentValidationUtil.validateConsentType(consentType, consentResource.getType());

        if ((consentResource.getRecurringIndicator() && AccountConsentUtil.isConsentExpired(
                consentResource.getValidityTime()))
                && !(StringUtils.equals(consentResource.getStatus(),
                ExtensionEnums.ConsentStatusEnum.TERMINATED_BY_TPP.toString())
                || StringUtils.equals(consentResource.getStatus(),
                ExtensionEnums.ConsentStatusEnum.REVOKED_BY_PSU.toString()))) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("[%s] The Consent is expired", requestId));
            }
            consentResource.setStatus(ExtensionEnums.ConsentStatusEnum.EXPIRED.toString());
        }

        JSONObject payloadToSend = new JSONObject();

        if (StringUtils.contains(requestPath, CommonConstants.STATUS)) {
            CommonConsentValidationUtil.appendConsentStatusResponse(consentResource, consentType, payloadToSend);
        } else {
            payloadToSend = CommonConsentValidationUtil.convertObjectToJson(consentResource.getReceipt());
            AccountConsentUtil.extendAccountConsentGetResponse(consentResource, payloadToSend);
        }

        SuccessResponseForResponseAlternation validationResponse = new SuccessResponseForResponseAlternation();
        validationResponse.setResponseId(requestBody.getRequestId());
        validationResponse.setStatus(SuccessResponseForResponseAlternation.StatusEnum.SUCCESS);
        validationResponse.setData(new SuccessResponseForResponseAlternationData()
                .modifiedResponse(payloadToSend)
                .responseHeaders(CommonConsentValidationUtil.getIdempotencyHeaderJSON(
                        headers.getString(CommonConstants.X_REQUEST_ID_HEADER))));

        return validationResponse;
    }

    /**
     * Handles revocation of account consents. This method is responsible for validating the revocation request,
     * checking if the consent is revocable and confirming the revocation.
     *
     * @param requestBody body of the request received by pre-process-consent-revocation
     * @return Successful validation result
     */
    @Override
    public SuccessResponseConsentRevocation handleRevocation(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        return CommonConsentValidationUtil.validateRevokeRequestAndReturnResponse(requestBody);
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
