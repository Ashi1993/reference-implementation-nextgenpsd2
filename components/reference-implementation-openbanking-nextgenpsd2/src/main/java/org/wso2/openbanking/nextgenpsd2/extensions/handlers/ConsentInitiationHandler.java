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

package org.wso2.openbanking.nextgenpsd2.extensions.handlers;

import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.EnrichConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseConsentRevocation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseForResponseAlternation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePreProcessConsentCreation;

/**
 * Consent handler interface for processing consent related requests.
 */
public interface ConsentInitiationHandler {

    /**
     * Handles the process of building successful validation result for consent creation requests.
     *
     * @param requestBody body of the request received by pre-process-consent-creation endpoint
     * @return Successful validation result
     * @throws ValidationFailureException if the consent creation request is invalid
     * @throws ExtensionException if an error occurred while validating the request
     */
    SuccessResponsePreProcessConsentCreation handleCreation(PreProcessConsentCreationRequestBody requestBody)
            throws ValidationFailureException, ExtensionException;

    /**
     * Handles the process of enriching the response for consent creation request.
     *
     * @param requestBody body of the request received by enrich-consent-creation-response endpoint
     * @return Response to forward
     * @throws ExtensionException if an error occurred while building the response
     */
    SuccessResponseForResponseAlternation enrichCreationResponse(EnrichConsentCreationRequestBody requestBody)
            throws ExtensionException;

    /**
     * Handles the process of building successful validation result for consent retrieval requests.
     *
     * @param requestBody body of the request received by pre-process-consent-retrieval
     * @return Successful retrieval response
     * @throws ValidationFailureException if the consent retrieval request is invalid
     * @throws ExtensionException if an error occurred while retrieving the consent
     */
    SuccessResponseForResponseAlternation handleRetrieval(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException;

    /**
     * Handles the process of building successful validation result for consent revocation requests.
     *
     * @param requestBody body of the request received by pre-process-consent-revocation
     * @return Successful validation result
     * @throws ValidationFailureException if the consent revocation request is invalid
     * @throws ExtensionException if an error occurred while validating the request
     */
    SuccessResponseConsentRevocation handleRevocation(PreProcessConsentRequestBody requestBody)
            throws ValidationFailureException, ExtensionException;
}
