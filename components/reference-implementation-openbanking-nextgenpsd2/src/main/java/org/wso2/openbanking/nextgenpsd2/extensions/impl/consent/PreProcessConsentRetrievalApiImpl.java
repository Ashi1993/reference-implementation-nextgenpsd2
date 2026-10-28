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
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for pre-processing the consent retrieval request.
 */
public class PreProcessConsentRetrievalApiImpl {

    private static final Log log = LogFactory.getLog(PreProcessConsentRetrievalApiImpl.class);

    /**
     * Method for returning the response for pre-processing consent retrieval request.
     *
     * @param requestBody request body from pre-process consent retrieval request
     * @return built pre-processed consent retrieval response
     */
    public static Response handlePreProcessConsentRetrieval(PreProcessConsentRequestBody requestBody) {
        String requestId = requestBody.getRequestId();
        try {
            // Validate X-request-ID header
            CommonConsentValidationUtil.validateRequestIdentificationHeader(requestBody.getData().getRequestHeaders());

            // Get validation response for consent retrieval based on consent type
            ConsentInitiationHandler consentInitiationHandler = CommonConsentValidationUtil
                    .getConsentInitiationHandler(requestBody.getData().getConsentResourcePath());

            SuccessResponseForResponseAlternation validationResponse = consentInitiationHandler
                    .handleRetrieval(requestBody);

            return Response.ok().entity(new JSONObject(validationResponse).toString()).build();

        } catch (ValidationFailureException e) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("[%s] Validation failed for consent retrieval. Returning failed response.",
                        requestId), e);
            }
            return Response.ok().entity(e.toJsonString()).build();
        }  catch (ExtensionException e) {
            log.error(String.format("[%s] An error occurred retrieving consent.", requestId), e);
            return Response.status(e.getStatus()).entity(e.toJsonString()).build();
        }
    }
}
