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

import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.AuthorizationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.DetailedConsentResourceDataWithAmendments;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PersistAuthorizedConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PopulateConsentAuthorizeScreenData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePopulateConsentAuthorizeScreenData;

/**
 * Consent handler interface for processing consent authorization related requests.
 */
public interface ConsentAuthorizationHandler {

    /**
     * Populates consent related information to be displayed on consent authorize screen.
     *
     * @param responseData data of the response object to populate consent authorize screen
     * @param requestData data of the request made to populate consent authorize screen
     */
    void populateBasicConsentData(SuccessResponsePopulateConsentAuthorizeScreenData responseData,
                                  PopulateConsentAuthorizeScreenData requestData)
            throws AuthorizationFailureException, ExtensionException;

    /**
     * Populates account related information associated with the consent to be displayed on consent authorize screen.
     *
     * @param responseData data of the response object to populate consent authorize screen
     * @param requestData data of the request made to populate consent authorize screen
     */
    void populateAccountsData(SuccessResponsePopulateConsentAuthorizeScreenData responseData,
                              PopulateConsentAuthorizeScreenData requestData)
            throws AuthorizationFailureException, ExtensionException;

    /**
     * Returns the amended consent resource based on user input to consent authorization page.
     *
     * @param requestBody body of the request received by persist-authorized-consent
     * @param authorizingResource authorization resource being authorized
     * @return amended consent resource
     */
    DetailedConsentResourceDataWithAmendments getAmendedConsentResource
            (PersistAuthorizedConsentRequestBody requestBody, StoredAuthorization authorizingResource)
            throws AuthorizationFailureException, ExtensionException;
}
