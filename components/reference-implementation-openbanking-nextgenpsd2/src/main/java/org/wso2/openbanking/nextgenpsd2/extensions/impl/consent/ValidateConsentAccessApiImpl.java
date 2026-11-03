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
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ConsentAccessContext;
import org.wso2.openbanking.nextgenpsd2.extensions.model.common.SuccessResponse;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.ValidateConsentAccessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentHandlerFactory;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionResponseUtil;

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

        ConsentAccessContext context = new ConsentAccessContext(
                validateConsentAccessRequestBody.getData().getAdditionalProperties());

        // Echoed back on error responses when the accelerator sends the request headers
        String xRequestId = context.getHeader(CommonConstants.X_REQUEST_ID_PROPER_CASE_HEADER);

        try {
            validateConsentAccess(requestId, validateConsentAccessRequestBody, context);

            return buildSuccessResponse(requestId);

        } catch (ValidationFailureException e) {
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] Validation failed for consent access. Returning failed response.", requestId),
                    e, Response.Status.OK, e.toJsonString(), xRequestId);
        } catch (ExtensionException e) {
            return ExtensionResponseUtil.buildErrorResponse(log,
                    String.format("[%s] An error occurred validating consent access.", requestId), e, e.getStatus(),
                    e.toJsonString(), xRequestId);
        }
    }

    /**
     * Validates that the data access request is allowed under the stored consent.
     *
     * @param requestId                        request identifier used for logging
     * @param validateConsentAccessRequestBody the ValidateConsentAccessApi request body
     * @param context                          context of the data access request
     * @throws ExtensionException         if the consent access cannot be validated
     * @throws ValidationFailureException if the data access request is not allowed under the consent
     */
    private static void validateConsentAccess(String requestId,
                                              ValidateConsentAccessRequestBody validateConsentAccessRequestBody,
                                              ConsentAccessContext context)
            throws ExtensionException, ValidationFailureException {
        StoredDetailedConsentResourceData consentResource =
                validateConsentAccessRequestBody.getData().getConsentResource();

        // Validate the headers and the token the data access request was made with
        ConsentValidationUtil.validateDataRequestContext(requestId, consentResource, context);

        // Validate that the request path belongs to the service the consent was initiated under
        if (context.hasResourcePath()) {
            ConsentValidationUtil.validateResourcePathAgainstConsentType(consentResource.getType(),
                    context.getResourcePath());
        }

        // Apply the consent type specific access rules
        ConsentValidationHandler consentValidationHandler = ConsentHandlerFactory
                .getConsentValidationHandler(consentResource.getType());

        consentValidationHandler.validateConsentAccess(validateConsentAccessRequestBody, context);
    }

    /**
     * Builds the response returned when the consent access is validated successfully.
     *
     * @param requestId request identifier echoed back as the response identifier
     * @return built success response
     */
    private static Response buildSuccessResponse(String requestId) {
        SuccessResponse successResponse = new SuccessResponse();
        successResponse.setResponseId(requestId);
        successResponse.setStatus(SuccessResponse.StatusEnum.SUCCESS);

        return Response.ok().entity(successResponse).build();
    }
}
