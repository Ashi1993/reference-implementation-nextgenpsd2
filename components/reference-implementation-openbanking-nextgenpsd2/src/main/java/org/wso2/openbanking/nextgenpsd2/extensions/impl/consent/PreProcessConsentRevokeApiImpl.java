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
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseConsentRevocation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;

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
        try {
            // Validate X-request-ID header
            // Enable forwarding of the specific header in accelerator configurations
            CommonConsentValidationUtil.validateRequestIdentificationHeader(requestBody.getData().getRequestHeaders());

            // Get validation response for consent revocation based on consent type
            ConsentInitiationHandler consentInitiationHandler = CommonConsentValidationUtil
                    .getConsentInitiationHandler(requestBody.getData().getConsentResourcePath());

            SuccessResponseConsentRevocation validationResponse = consentInitiationHandler
                    .handleRevocation(requestBody);

            return Response.ok().entity(new JSONObject(validationResponse).toString()).build();

        } catch (ValidationFailureException e) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("[%s] Validation failed for consent revocation. Returning failed response.",
                        requestId), e);
            }
            return Response.ok().entity(e.toJsonString()).build();
        }  catch (ExtensionException e) {
            log.error(String.format("[%s] An error occurred revoking consent.", requestId), e);
            return Response.status(e.getStatus()).entity(e.toJsonString()).build();
        }
    }
}
