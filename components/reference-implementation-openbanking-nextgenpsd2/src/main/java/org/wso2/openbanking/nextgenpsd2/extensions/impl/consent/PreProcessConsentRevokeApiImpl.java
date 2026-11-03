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
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseConsentRevocation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentHandlerFactory;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionResponseUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.HeaderUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for pre-processing the consent revocation request.
 */
public class PreProcessConsentRevokeApiImpl {

    private static final Log log = LogFactory.getLog(PreProcessConsentRevokeApiImpl.class);

    /**
     * Method for returning response for pre-processing consent revocation request.
     *
     * @param requestBody request body from pre-process consent revocation request
     * @return built pre-processed consent revocation response
     */
    public static Response handlePreProcessConsentRevoke(PreProcessConsentRequestBody requestBody) {
        String requestId = requestBody.getRequestId();

        // Echoed back on error responses. Remains null until the X-Request-ID header is known to be valid.
        String xRequestId = null;

        try {
            // Validate X-request-ID header
            // Enable forwarding of the specific header in accelerator configurations
            HeaderUtil.validateRequestIdentificationHeader(requestBody.getData().getRequestHeaders());
            xRequestId = HeaderUtil.getXRequestId(requestBody.getData().getRequestHeaders());

            return ExtensionResponseUtil.buildSuccessResponse(revokeConsent(requestBody));

        } catch (ValidationFailureException e) {
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] Validation failed for consent revocation. Returning failed response.",
                            requestId), e, Response.Status.OK, e.toJsonString(), xRequestId);
        }  catch (ExtensionException e) {
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] An error occurred revoking consent.", requestId), e, e.getStatus(),
                    e.toJsonString(), xRequestId);
        }
    }

    /**
     * Revokes the consent using the initiation handler matching the consent type of the request.
     *
     * @param requestBody request body from pre-process consent revocation request
     * @return consent revocation response produced by the handler
     * @throws ExtensionException         if the consent cannot be revoked
     * @throws ValidationFailureException if the request does not map to a supported consent type
     */
    private static SuccessResponseConsentRevocation revokeConsent(PreProcessConsentRequestBody requestBody)
            throws ExtensionException, ValidationFailureException {
        ConsentInitiationHandler consentInitiationHandler = ConsentHandlerFactory
                .getConsentInitiationHandler(requestBody.getData().getConsentResourcePath());

        return consentInitiationHandler.handleRevocation(requestBody);
    }
}
