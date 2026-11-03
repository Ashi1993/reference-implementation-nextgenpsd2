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
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PopulateConsentAuthorizeScreenData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PopulateConsentAuthorizeScreenRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePopulateConsentAuthorizeScreen;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePopulateConsentAuthorizeScreenData;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentAuthorizationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentHandlerFactory;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionResponseUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.JsonUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for populating the consent authorize screen.
 */
public class PopulateConsentAuthorizeScreenApiImpl {

    private static final Log log = LogFactory.getLog(PopulateConsentAuthorizeScreenApiImpl.class);

    /**
     * Method for returning the response for populating consent authorization page.
     *
     * @param requestBody request made to populate-consent-authorize-screen
     * @return payload required to generate the custom consent authorization page
     */
    public static Response handlePopulateConsentAuthorizeScreen(PopulateConsentAuthorizeScreenRequestBody requestBody) {
        String requestId = requestBody.getRequestId();
        PopulateConsentAuthorizeScreenData data = requestBody.getData();
        StoredDetailedConsentResourceData consentResource = data.getConsentResource();

        try {
            JSONObject queryParams = JsonUtil.convertObjectToJson(data.getRequestParameters());

            // Verify client
            CommonConsentValidationUtil.validateAuthorizingClient(consentResource, queryParams);

            // Validate consent type with consent scopes
            CommonConsentValidationUtil.validateScope(requestId, queryParams, consentResource.getType());

            // Check if authorizable
            StoredAuthorization unauthorizedObj = ConsentAuthorizationUtil
                    .getAuthorizableResource(requestId, consentResource.getAuthorizations(), data.getUserId(),
                            consentResource);

            ConsentAuthorizationHandler authorizationHandler = ConsentHandlerFactory.getAuthorizationHandler(
                    consentResource.getType());

            return ExtensionResponseUtil.buildSerializedSuccessResponse(
                    buildAuthorizeScreenResponse(requestId, data, authorizationHandler, unauthorizedObj));

        } catch (AuthorizationFailureException e) {
            // The failure is carried back to the PSU through the redirect URL, thus returned with an OK status
            e.setResponseId(requestId);
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] Authorization retrieval failed. Redirecting to redirect URL with "
                            + "error description", requestId), e, Response.Status.OK, e.toJsonString(), null);
        } catch (ExtensionException e) {
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] An error occurred populating consent authorize screen.", requestId), e,
                    e.getStatus(), e.toJsonString(), null);
        }
    }

    /**
     * Builds the payload the custom consent authorization page is generated from.
     *
     * @param requestId            request identifier echoed back as the response identifier
     * @param data                 data of the populate consent authorize screen request
     * @param authorizationHandler authorization handler matching the consent type of the request
     * @param unauthorizedObj      the authorization awaiting the user decision
     * @return success response for populating the consent authorize screen
     * @throws ExtensionException            if the consent data to display cannot be built
     * @throws AuthorizationFailureException if the consent cannot be presented for authorization
     */
    private static SuccessResponsePopulateConsentAuthorizeScreen buildAuthorizeScreenResponse(
            String requestId, PopulateConsentAuthorizeScreenData data,
            ConsentAuthorizationHandler authorizationHandler, StoredAuthorization unauthorizedObj)
            throws ExtensionException, AuthorizationFailureException {
        SuccessResponsePopulateConsentAuthorizeScreenData responseData =
                new SuccessResponsePopulateConsentAuthorizeScreenData();

        // Add basic consent data to display
        authorizationHandler.populateBasicConsentData(responseData, data);

        // Add account data to display
        authorizationHandler.populateAccountsData(responseData, data);

        // Append authorizable consent as consent metadata
        CommonConsentValidationUtil.appendAuthorizationToResponse(responseData, unauthorizedObj);

        SuccessResponsePopulateConsentAuthorizeScreen response =
                new SuccessResponsePopulateConsentAuthorizeScreen();
        response.setResponseId(requestId);
        response.setStatus(SuccessResponsePopulateConsentAuthorizeScreen.StatusEnum.SUCCESS);
        response.setData(responseData);

        return response;
    }
}
