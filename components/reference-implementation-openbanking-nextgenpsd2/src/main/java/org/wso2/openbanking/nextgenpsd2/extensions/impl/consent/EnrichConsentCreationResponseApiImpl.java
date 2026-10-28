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
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.EnrichConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for enriching the consent creation response.
 */
public class EnrichConsentCreationResponseApiImpl {

    private static final Log log = LogFactory.getLog(EnrichConsentCreationResponseApiImpl.class);

    /**
     * Method for returning the response for enriching consent creation request.
     *
     * @param requestBody request body from enrich consent creation request
     * @return built enrich consent creation response
     */
    public static Response handleEnrichConsentCreationResponse(EnrichConsentCreationRequestBody requestBody) {
        String requestId = requestBody.getRequestId();
        try {
            ConsentInitiationHandler consentHandler = CommonConsentValidationUtil
                    .getConsentInitiationHandler(requestBody.getData().getConsentResourcePath());
            SuccessResponseForResponseAlternation validationResponse = consentHandler
                    .enrichCreationResponse(requestBody);

            return Response.ok().entity(new JSONObject(validationResponse).toString()).build();

        } catch (ExtensionException e) {
            log.error(String.format("[%s] An error occurred enriching consent creation response.", requestId), e);
            return Response.status(e.getStatus()).entity(e.toJsonString()).build();
        } catch (ValidationFailureException e) {
            // Should be unreachable since resource path is validated in consent creation
            // Thus bad request error is thrown
            log.error(String.format("[%s] Invalid resource path received when enriching consent creation response.",
                    requestId), e);
            return Response.status(Response.Status.BAD_REQUEST).entity(ErrorUtil.getErrorResponse(
                    "invalid_request", "Invalid resource path received when enriching " +
                            "consent creation response.")).build();
        }
    }
}
