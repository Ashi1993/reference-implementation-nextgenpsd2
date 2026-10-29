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
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ConsentAccessContext;
import org.wso2.openbanking.nextgenpsd2.extensions.model.common.SuccessResponse;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.ValidateConsentAccessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentValidationUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for validating consent access.
 */
public class ValidateConsentAccessApiImpl {

    private static final Log log = LogFactory.getLog(ValidateConsentAccessApiImpl.class);

    /**
     * Handles validating consent access.
     *
     * @param validateConsentAccessRequestBody the ValidateConsentAccessApi request body
     * @return the built response
     */
    public static Response handleConsentAccessValidation(
            ValidateConsentAccessRequestBody validateConsentAccessRequestBody) {

        String requestId = validateConsentAccessRequestBody.getRequestId();
        try {
            StoredDetailedConsentResourceData consentResource =
                    validateConsentAccessRequestBody.getData().getConsentResource();

            ConsentAccessContext context = new ConsentAccessContext(
                    validateConsentAccessRequestBody.getData().getAdditionalProperties());

            // Validate the headers and the token the data access request was made with
            ConsentValidationUtil.validateDataRequestContext(requestId, consentResource, context);

            // Validate that the request path belongs to the service the consent was initiated under
            if (context.hasResourcePath()) {
                ConsentValidationUtil.validateResourcePathAgainstConsentType(consentResource.getType(),
                        context.getResourcePath());
            }

            ConsentValidationHandler consentValidationHandler = CommonConsentValidationUtil
                    .getConsentValidationHandler(consentResource.getType());

            consentValidationHandler.validateConsentAccess(validateConsentAccessRequestBody, context);

            SuccessResponse successResponse = new SuccessResponse();
            successResponse.setResponseId(requestId);
            successResponse.setStatus(SuccessResponse.StatusEnum.SUCCESS);

            return Response.ok().entity(successResponse).build();

        } catch (ValidationFailureException e) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("[%s] Validation failed for consent access. Returning failed response.",
                        requestId), e);
            }
            return Response.ok().entity(e.toJsonString()).build();
        } catch (ExtensionException e) {
            log.error(String.format("[%s] An error occurred validating consent access.", requestId), e);
            return Response.status(e.getStatus()).entity(e.toJsonString()).build();
        }
    }
}
