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
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.EnrichConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentHandlerFactory;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionResponseUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for enriching the consent creation response.
 */
public class EnrichConsentCreationResponseApiImpl {

    private static final Log log = LogFactory.getLog(EnrichConsentCreationResponseApiImpl.class);

    private static final String INVALID_RESOURCE_PATH_ERROR =
            "Invalid resource path received when enriching consent creation response.";

    /**
     * Method for returning the response for enriching consent creation request.
     *
     * @param requestBody request body from enrich consent creation request
     * @return built enrich consent creation response
     */
    public static Response handleEnrichConsentCreationResponse(EnrichConsentCreationRequestBody requestBody) {
        String requestId = requestBody.getRequestId();

        try {
            return ExtensionResponseUtil.buildSuccessResponse(enrichCreationResponse(requestBody));

        } catch (ExtensionException e) {
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] An error occurred enriching consent creation response.", requestId), e,
                    e.getStatus(), e.toJsonString(), null);
        } catch (ValidationFailureException e) {
            // Should be unreachable since resource path is validated in consent creation
            // Thus bad request error is thrown
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] %s", requestId, INVALID_RESOURCE_PATH_ERROR), e, Response.Status.BAD_REQUEST,
                    ErrorUtil.getErrorResponse(ErrorConstants.INVALID_REQUEST, INVALID_RESOURCE_PATH_ERROR), null);
        }
    }

    /**
     * Enriches the creation response using the initiation handler matching the consent type of the request.
     *
     * @param requestBody request body from enrich consent creation request
     * @return enriched creation response produced by the handler
     * @throws ExtensionException         if the response cannot be enriched
     * @throws ValidationFailureException if the request does not map to a supported consent type
     */
    private static SuccessResponseForResponseAlternation enrichCreationResponse(
            EnrichConsentCreationRequestBody requestBody) throws ExtensionException, ValidationFailureException {

        ConsentInitiationHandler consentHandler = ConsentHandlerFactory
                .getConsentInitiationHandler(requestBody.getData().getConsentResourcePath());

        return consentHandler.enrichCreationResponse(requestBody);
    }
}
