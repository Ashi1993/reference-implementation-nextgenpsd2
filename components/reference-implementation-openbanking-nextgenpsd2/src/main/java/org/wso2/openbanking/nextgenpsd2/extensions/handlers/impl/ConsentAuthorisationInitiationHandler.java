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

package org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl;

import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.EnrichConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseConsentRevocation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePreProcessConsentCreation;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;

import javax.ws.rs.core.Response;

/**
 * Consent authorisation handler for explicit authorisation.
 */
public class ConsentAuthorisationInitiationHandler implements ConsentInitiationHandler {
    // ToDo: Implement authorisation creation for consents

    /**
     * Handle creation of authorisations for consents. This method is responsible for completing specification specific
     * explicit authorisation creation request.
     *
     * <p><b>Note:</b> Creation of explicit authorizations to existing consents is not supported</p>
     *
     * @param requestBody body of the request received by pre-process-consent-creation endpoint
     * @return Successful validation result
     */
    @Override
    public SuccessResponsePreProcessConsentCreation handleCreation(PreProcessConsentCreationRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        // Throws an error since creating auth object for an existing consent is not supported
        throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                        TPPMessage.CodeEnum.SERVICE_INVALID_405, ErrorConstants.AUTH_CREATION_NOT_SUPPORTED));
    }

    /**
     * Handles retrieval of consent authorisations. This method is responsible for validating the requested
     * authorisation resource and building the response encompassing authorisation information as specified by the
     * specification.
     *
     * <p><b>Note:</b> Retrieval of explicit authorization resources cannot be done given creating explicit
     * authorizations to existing consents is not supported</p>
     *
     * @param requestBody body of the request received by pre-process-consent-retrieval
     * @return Successful retrieval response
     */
    @Override
    public SuccessResponseForResponseAlternation handleRetrieval(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        // Throws an error since auth resources for a consent cannot be retrieved through consent creation
        throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                        TPPMessage.CodeEnum.SERVICE_INVALID_405, ErrorConstants.DELETE_NOT_SUPPORTED));
    }

    /**
     * Handles removal of consent authorizations. This method is responsible for validating the removal request,
     * checking if the authorization resource is removable and confirming the removal.
     *
     * <p><b>Note:</b> Removal of explicit authorization resources cannot be done given creating explicit
     * authorizations to existing consents is not supported</p>
     *
     * @param requestBody body of the request received by pre-process-consent-revocation
     * @return Successful validation result
     */
    @Override
    public SuccessResponseConsentRevocation handleRevocation(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException {
        // Throws an error since auth resources for a consent cannot be retrieved through consent creation
        throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                        TPPMessage.CodeEnum.SERVICE_INVALID_405, ErrorConstants.AUTH_CREATION_NOT_SUPPORTED));
    }

    /**
     * Handles consent authorization creation response customization. Upon a successful creation of an authorization
     * resource, this method is responsible for building the specification specified authorization resource creation
     * response.
     *
     * <p><b>Note:</b> Response enrichment for explicit authorization resource creation cannot be done given creating
     * explicit authorizations to existing consents is not supported</p>
     *
     * @param requestBody body of the request received by enrich-consent-creation-response endpoint
     * @return Response to forward
     */
    @Override
    public SuccessResponseForResponseAlternation enrichCreationResponse(EnrichConsentCreationRequestBody requestBody)
            throws ExtensionException {
        throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                ErrorConstants.AUTH_CREATION_NOT_SUPPORTED);
    }
}
