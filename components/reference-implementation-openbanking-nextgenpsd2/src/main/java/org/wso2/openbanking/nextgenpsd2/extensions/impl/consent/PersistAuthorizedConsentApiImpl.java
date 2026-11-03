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

package org.wso2.openbanking.nextgenpsd2.extensions.impl.consent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.AuthorizationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentAuthorizationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PersistAuthorizedConsent;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PersistAuthorizedConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePersistAuthorizedConsent;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePersistAuthorizedConsentData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.UserGrantedData;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentHandlerFactory;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionResponseUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.JsonUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.PaymentConsentUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for persisting the authorized consent.
 */
public class PersistAuthorizedConsentApiImpl {

    private static final Log log = LogFactory.getLog(PersistAuthorizedConsentApiImpl.class);

    /**
     * Method for building persist authorized consent response.
     *
     * @param requestBody request made to persist-authorized-consent endpoint
     * @return response containing any modified consent information and account mappings with permissions
     */
    public static Response handlePersistAuthorizedConsent(PersistAuthorizedConsentRequestBody requestBody) {
        String requestId = requestBody.getRequestId();
        PersistAuthorizedConsent persistData = requestBody.getData();
        StoredDetailedConsentResourceData consentResource = persistData.getConsentResource();

        try {
            StoredAuthorization authorizingResource = extractAuthorizingResource(persistData);

            // Banking backend integration for payments
            if (persistData.getIsApproved()) {
                PaymentConsentUtil.handleBackendPayment(authorizingResource, consentResource);
            }

            return ExtensionResponseUtil.buildSerializedSuccessResponse(
                    buildPersistedConsentResponse(requestBody, authorizingResource));

        } catch (AuthorizationFailureException e) {
            // The failure is carried back to the PSU through the redirect URL, thus returned with an OK status
            e.setResponseId(requestId);
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] Authorization persistence failed. Redirecting to redirect URL with "
                            + "error description", requestId), e, Response.Status.OK, e.toJsonString(), null);
        } catch (ExtensionException e) {
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] An error occurred persisting authorized consent.", requestId), e,
                    e.getStatus(), e.toJsonString(), null);
        }
    }

    /**
     * Restores the authorization being authorized from the metadata carried by the user granted data.
     *
     * @param persistData data of the persist authorized consent request
     * @return the authorization the user is authorizing
     * @throws ExtensionException if the metadata cannot be read as JSON or carries no authorizing authorization
     */
    private static StoredAuthorization extractAuthorizingResource(PersistAuthorizedConsent persistData)
            throws ExtensionException {
        UserGrantedData userGrantedData = persistData.getUserGrantedData();
        JSONObject retrievalMetadata = JsonUtil
                .convertObjectToJson(userGrantedData.getAuthorizedResources().getMetadata());

        return CommonConsentValidationUtil.extractAuthorizingResource(retrievalMetadata);
    }

    /**
     * Builds the success response carrying the consent resource as amended by the authorization handler.
     *
     * @param requestBody         request made to persist-authorized-consent endpoint
     * @param authorizingResource the authorization the user is authorizing
     * @return success response for consent persistence
     * @throws ExtensionException            if the amended consent resource cannot be built
     * @throws AuthorizationFailureException if the consent cannot be amended for the authorization
     */
    private static SuccessResponsePersistAuthorizedConsent buildPersistedConsentResponse(
            PersistAuthorizedConsentRequestBody requestBody, StoredAuthorization authorizingResource)
            throws ExtensionException, AuthorizationFailureException {
        String consentType = requestBody.getData().getConsentResource().getType();
        ConsentAuthorizationHandler authorizationHandler =
                ConsentHandlerFactory.getAuthorizationHandler(consentType);

        SuccessResponsePersistAuthorizedConsentData responseData = new SuccessResponsePersistAuthorizedConsentData();
        responseData.setConsentResource(authorizationHandler.getAmendedConsentResource(requestBody,
                authorizingResource));

        SuccessResponsePersistAuthorizedConsent response = new SuccessResponsePersistAuthorizedConsent();
        response.setResponseId(requestBody.getRequestId());
        response.setStatus(SuccessResponsePersistAuthorizedConsent.StatusEnum.SUCCESS);
        response.setData(responseData);

        return response;
    }
}
