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

package org.wso2.openbanking.nextgenpsd2.extensions.handlers;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ExtensionConfiguration;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.Authorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.DetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRetrievalData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredBasicConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseConsentRevocation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternationData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePreProcessConsentCreation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseWithDetailedConsentData;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.HeaderUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.JsonUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ScaUtil;

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

/**
 * Base of the consent initiation handlers, carrying the creation, retrieval and revocation of a consent as the
 * nextGenPSD2 specification defines them for every consent type.
 * <p>
 * A handler contributes the parts that differ between the consent types it serves, which are the validation of
 * the initiation payload, the type and the initial status of the created consent, the consent parameters that
 * apply to it, and the response its retrieval returns.
 */
public abstract class AbstractConsentInitiationHandler implements ConsentInitiationHandler {

    private static final Log log = LogFactory.getLog(AbstractConsentInitiationHandler.class);

    /**
     * Handles creation of a consent, completing the specification specific validations on the initiation
     * payload and building the consent resource the accelerator stores.
     *
     * @param requestBody body of the request received by pre-process-consent-creation endpoint
     * @return successful validation result
     * @throws ValidationFailureException if the request does not meet the specification
     * @throws ExtensionException if the consent to create cannot be built
     */
    @Override
    public SuccessResponsePreProcessConsentCreation handleCreation(PreProcessConsentCreationRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {

        String requestId = requestBody.getRequestId();

        // Skipping idempotency check as it's handled by the accelerator
        // ToDo: Add explicit authorisation support

        JSONObject headers = JsonUtil.convertObjectToJson(requestBody.getData().getRequestHeaders());

        validateCreationHeaders(requestId, headers);

        JSONObject requestPayload = JsonUtil.convertObjectToJson(requestBody.getData().getConsentInitiationData());
        validateInitiationPayload(requestPayload, requestBody);

        Optional<Boolean> isRedirectPreferred = HeaderUtil.isTppRedirectPreferred(requestId, headers);
        if (isRedirectPreferred.isPresent() && BooleanUtils.isFalse(isRedirectPreferred.get())) {
            //ToDo: revisit once decoupled approach is implemented.
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, String.format("%s SCA Approach is not supported",
                                    ExtensionEnums.ScaApproachEnum.DECOUPLED)));
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] SCA approach is Redirect SCA (OAuth2)", requestId));
        }

        SuccessResponseWithDetailedConsentData responseData = new SuccessResponseWithDetailedConsentData();
        responseData.setConsentResource(buildConsentToCreate(requestBody, requestPayload, headers));

        SuccessResponsePreProcessConsentCreation creationResponse = new SuccessResponsePreProcessConsentCreation();
        creationResponse.setResponseId(requestId);
        creationResponse.setStatus(SuccessResponsePreProcessConsentCreation.StatusEnum.SUCCESS);
        creationResponse.setData(responseData);

        return creationResponse;
    }

    /**
     * Builds the consent resource the accelerator stores for the creation request.
     *
     * @param requestBody    body of the request received by pre-process-consent-creation endpoint
     * @param requestPayload initiation payload of the consent being created
     * @param headers        request headers of the creation request
     * @return the consent resource to store
     * @throws ValidationFailureException if the request does not meet the specification
     * @throws ExtensionException if the consent resource cannot be built
     */
    private DetailedConsentResourceData buildConsentToCreate(PreProcessConsentCreationRequestBody requestBody,
                                                             JSONObject requestPayload, JSONObject headers)
            throws ValidationFailureException, ExtensionException {

        DetailedConsentResourceData consentResource = new DetailedConsentResourceData();
        consentResource.setReceipt(requestPayload);
        consentResource.setType(getConsentType(requestBody));
        consentResource.setStatus(getInitialConsentStatus());
        consentResource.setAttributes(getConsentAttributes(requestBody, headers));
        setConsentValidity(consentResource, requestPayload);

        // Build auth resource for implicit authorisation
        // ToDo: Revisit once explicit authorisation is supported
        consentResource.addAuthorizationsItem(buildImplicitAuthorization(headers));

        return consentResource;
    }

    /**
     * Builds the authorization the consent is implicitly authorized through.
     *
     * @param headers request headers of the creation request
     * @return the authorization to store with the consent
     */
    private Authorization buildImplicitAuthorization(JSONObject headers) {
        boolean isScaRequired = ExtensionConfiguration.isScaRequired();

        Authorization authorization = new Authorization();
        if (headers.has(CommonConstants.PSU_ID_HEADER)) {
            authorization.setUserId(headers.getString(CommonConstants.PSU_ID_HEADER));
        }
        authorization.setType(ExtensionEnums.AuthTypeEnum.AUTHORISATION.toString());
        authorization.setStatus(ScaUtil.getAuthorizationStatus(isScaRequired, headers));

        return authorization;
    }

    /**
     * Handles retrieval of a consent, validating that the consent may be retrieved by the requesting client and
     * building the response the specification defines for it.
     *
     * @param requestBody body of the request received by pre-process-consent-retrieval
     * @return successful retrieval response
     * @throws ValidationFailureException if the consent may not be retrieved
     * @throws ExtensionException if the retrieval response cannot be built
     */
    @Override
    public SuccessResponseForResponseAlternation handleRetrieval(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {

        String requestId = requestBody.getRequestId();
        PreProcessConsentRetrievalData data = requestBody.getData();
        StoredBasicConsentResourceData consentResource = data.getConsentResource();

        JSONObject headers = JsonUtil.convertObjectToJson(data.getRequestHeaders());

        // Validate client
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating consent of Id %s for valid client", requestId,
                    data.getConsentId()));
        }
        CommonConsentValidationUtil.validateClient(HeaderUtil.getClientId(headers), consentResource.getClientId());

        // Validate consent type
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating consent of Id %s for correct type", requestId,
                    data.getConsentId()));
        }
        CommonConsentValidationUtil.validateConsentType(getRetrievedConsentType(requestBody),
                consentResource.getType());

        validateRetrieval(requestId, requestBody);

        SuccessResponseForResponseAlternationData responseData = new SuccessResponseForResponseAlternationData();
        responseData.setModifiedResponse(buildRetrievalPayload(requestBody));
        responseData.setResponseHeaders(HeaderUtil.getIdempotencyHeaderJSON(
                headers.getString(CommonConstants.X_REQUEST_ID_HEADER)));

        SuccessResponseForResponseAlternation retrievalResponse = new SuccessResponseForResponseAlternation();
        retrievalResponse.setResponseId(requestId);
        retrievalResponse.setStatus(SuccessResponseForResponseAlternation.StatusEnum.SUCCESS);
        retrievalResponse.setData(responseData);

        return retrievalResponse;
    }

    /**
     * Builds the payload the retrieval of the consent returns, which carries the consent status alone on a
     * status request and the consent receipt otherwise.
     *
     * @param requestBody body of the request received by pre-process-consent-retrieval
     * @return the payload to return
     * @throws ValidationFailureException if the request does not map to a consent type
     * @throws ExtensionException if the payload cannot be built
     */
    protected JSONObject buildRetrievalPayload(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        StoredBasicConsentResourceData consentResource = requestBody.getData().getConsentResource();

        JSONObject payloadToSend = new JSONObject();
        if (!requestBody.getData().getConsentResourcePath().contains(CommonConstants.STATUS)) {
            payloadToSend = JsonUtil.convertObjectToJson(consentResource.getReceipt());
        }
        CommonConsentValidationUtil.appendConsentStatusResponse(consentResource,
                getRetrievedConsentType(requestBody), payloadToSend);

        return payloadToSend;
    }

    /**
     * Handles revocation of a consent, validating the revocation request and confirming the revocation.
     *
     * @param requestBody body of the request received by pre-process-consent-revocation
     * @return successful validation result
     * @throws ValidationFailureException if the consent may not be revoked
     * @throws ExtensionException if the revocation response cannot be built
     */
    @Override
    public SuccessResponseConsentRevocation handleRevocation(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        return CommonConsentValidationUtil.validateRevokeRequestAndReturnResponse(requestBody);
    }

    /**
     * Validates the headers of a creation request. Every consent type requires the TPP-Redirect-Preferred
     * header to name a supported SCA approach.
     *
     * @param requestId correlation identifier of the request
     * @param headers   request headers of the creation request
     * @throws ValidationFailureException if a header does not meet the specification
     * @throws ExtensionException if construction of an error failed
     */
    protected void validateCreationHeaders(String requestId, JSONObject headers)
            throws ValidationFailureException, ExtensionException {
        HeaderUtil.validateTppRedirectPreferredHeader(requestId, headers);
    }

    /**
     * Validates the initiation payload of the consent being created against the specification.
     *
     * @param requestPayload initiation payload of the consent being created
     * @param requestBody    body of the request received by pre-process-consent-creation endpoint
     * @throws ValidationFailureException if the payload does not meet the specification
     * @throws ExtensionException if construction of an error failed
     */
    protected abstract void validateInitiationPayload(JSONObject requestPayload,
                                                      PreProcessConsentCreationRequestBody requestBody)
            throws ValidationFailureException, ExtensionException;

    /**
     * The type of the consent being created.
     *
     * @param requestBody body of the request received by pre-process-consent-creation endpoint
     * @return the consent type to store
     * @throws ValidationFailureException if the request does not map to a consent type
     * @throws ExtensionException if construction of an error failed
     */
    protected abstract String getConsentType(PreProcessConsentCreationRequestBody requestBody)
            throws ValidationFailureException, ExtensionException;

    /**
     * The status a consent of this type carries when it is created.
     *
     * @return the initial status of the consent
     */
    protected abstract String getInitialConsentStatus();

    /**
     * The type the consent being retrieved is expected to carry.
     *
     * @param requestBody body of the request received by pre-process-consent-retrieval
     * @return the expected consent type
     * @throws ValidationFailureException if the request does not map to a consent type
     * @throws ExtensionException if construction of an error failed
     */
    protected abstract String getRetrievedConsentType(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException;

    /**
     * Sets the validity of the consent being created. A consent that is valid for a single use carries no
     * validity of its own, which is the case for every consent type but an account consent.
     *
     * @param consentResource consent resource being built
     * @param requestPayload  initiation payload of the consent being created
     * @throws ValidationFailureException if the validity requested by the payload is invalid
     * @throws ExtensionException if construction of an error failed
     */
    protected void setConsentValidity(DetailedConsentResourceData consentResource, JSONObject requestPayload)
            throws ValidationFailureException, ExtensionException {
        consentResource.setFrequency(0);
        consentResource.setValidityTime(0L);
        consentResource.setRecurringIndicator(false);
    }

    /**
     * The attributes stored with the consent being created. The X-Request-ID of the creation request is stored
     * for every consent type, since the creation response is built from it.
     *
     * @param requestBody body of the request received by pre-process-consent-creation endpoint
     * @param headers     request headers of the creation request
     * @return the attributes to store with the consent
     * @throws ValidationFailureException if an attribute cannot be read from the request
     * @throws ExtensionException if an attribute cannot be read from the request
     */
    protected Map<String, Object> getConsentAttributes(PreProcessConsentCreationRequestBody requestBody,
                                                       JSONObject headers)
            throws ValidationFailureException, ExtensionException {
        return Map.of(CommonConstants.X_REQUEST_ID_HEADER, headers.getString(CommonConstants.X_REQUEST_ID_HEADER));
    }

    /**
     * Validates a retrieval request beyond the client and the consent type, which no consent type requires by
     * default.
     *
     * @param requestId   correlation identifier of the request
     * @param requestBody body of the request received by pre-process-consent-retrieval
     * @throws ValidationFailureException if the consent may not be retrieved
     * @throws ExtensionException if construction of an error failed
     */
    protected void validateRetrieval(String requestId, PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        // No further validation is required by default
    }
}
