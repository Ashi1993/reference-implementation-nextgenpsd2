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
    public static Response handlePersistAuthorizedConsent(PersistAuthorizedConsentRequestBody
                                                            requestBody) {
        String requestId = requestBody.getRequestId();
        PersistAuthorizedConsent persistData = requestBody.getData();
        boolean isApproved = persistData.getIsApproved();
        UserGrantedData userGrantedData = persistData.getUserGrantedData();
        StoredDetailedConsentResourceData consentResource = persistData.getConsentResource();
        String consentType = consentResource.getType();

        try {
            JSONObject retrievalMetadata = CommonConsentValidationUtil
                    .convertObjectToJson(userGrantedData.getAuthorizedResources().getMetadata());

            // Restore authorizing authorization
            StoredAuthorization authorizingResource = CommonConsentValidationUtil
                    .extractAuthorizingResource(retrievalMetadata);

            // Banking backend integration for payments
            if (isApproved) {
                PaymentConsentUtil.handleBackendPayment(authorizingResource, consentResource);
            }

            // Build success response for consent persistence
            SuccessResponsePersistAuthorizedConsent response = new SuccessResponsePersistAuthorizedConsent();
            response.setResponseId(requestId);
            response.setStatus(SuccessResponsePersistAuthorizedConsent.StatusEnum.SUCCESS);
            SuccessResponsePersistAuthorizedConsentData responseData =
                    new SuccessResponsePersistAuthorizedConsentData();

            ConsentAuthorizationHandler authorizationHandler =
                    CommonConsentValidationUtil.getAuthorizationHandler(consentType);

            responseData.setConsentResource(authorizationHandler.getAmendedConsentResource(requestBody,
                    authorizingResource));

            response.setData(responseData);

            return CommonConsentValidationUtil.buildPersistResponseFromObject(response);

        } catch (AuthorizationFailureException e) {
            log.error(String.format("[%s] Authorization persistence failed. Redirecting to redirect URL with " +
                    "error description", requestId), e);
            e.setResponseId(requestId);
            return Response.status(Response.Status.OK).entity(e.toJsonString()).build();
        } catch (ExtensionException e) {
            log.error(String.format("[%s] An error occurred persisting authorized consent.", requestId), e);
            return Response.status(e.getStatus()).entity(e.toJsonString()).build();
        }
    }
}
