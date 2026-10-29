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

package org.wso2.openbanking.nextgenpsd2.extensions.handlers;

import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ConsentAccessContext;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.ValidateConsentAccessRequestBody;

/**
 * Consent handler interface for validating data access against a consent.
 */
public interface ConsentValidationHandler {

    /**
     * Validates whether the data access request is permitted by the consent. The validation is a gate only, since
     * the validate-consent-access response carries no data on success.
     *
     * @param requestBody body of the request received by the validate-consent-access endpoint
     * @param context context of the data access request, which may be absent
     * @throws ValidationFailureException if the data access request is not permitted by the consent
     * @throws ExtensionException if an error occurred while validating the request
     */
    void validateConsentAccess(ValidateConsentAccessRequestBody requestBody, ConsentAccessContext context)
            throws ValidationFailureException, ExtensionException;
}
