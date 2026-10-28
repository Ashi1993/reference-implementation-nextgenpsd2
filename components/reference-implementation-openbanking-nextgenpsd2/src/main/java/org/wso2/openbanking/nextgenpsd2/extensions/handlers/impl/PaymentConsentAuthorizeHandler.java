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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.AuthorizationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentAuthorizationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.common.Resource;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.AuthorizedResourcesAuthorizedDataInner;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.DetailedConsentResourceDataWithAmendments;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PersistAuthorizedConsent;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PersistAuthorizedConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PopulateConsentAuthorizeScreenData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePopulateConsentAuthorizeScreenData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePopulateConsentAuthorizeScreenDataConsentData;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.AccountConsentUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConsentValidationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ConsentAuthorizationUtil;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ExtensionEnums;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.PaymentConsentUtil;

import java.util.List;

import javax.ws.rs.core.Response;

/**
 * Consent handler interface for processing account consent authorization related requests.
 */
public class PaymentConsentAuthorizeHandler implements ConsentAuthorizationHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Return basic consent data for payment consents.
     * This includes extracting mandatory consent data (excluding any account data) to be shown in the consent
     * authorization page.
     * This method handles data population for payment service types,
     * <ul>
     *     <li>single payment</li>
     *     <li>bulk payment</li>
     *     <li>periodic payment</li>
     * </ul>
     *
     * @param responseData data of the response object to populate consent authorize screen for payment consents
     * @param requestData data of the request made to populate-consent-authorize-screen for payment consents
     */
    @Override
    public void populateBasicConsentData(SuccessResponsePopulateConsentAuthorizeScreenData responseData,
                                         PopulateConsentAuthorizeScreenData requestData) throws ExtensionException {
        // Add consent data if null
        if (responseData.getConsentData() == null) {
            responseData.setConsentData(new SuccessResponsePopulateConsentAuthorizeScreenDataConsentData());
        }

        responseData.getConsentData().setType(requestData.getConsentResource().getType());

        // Populate basic consent data based on payment type
        String consentType = requestData.getConsentResource().getType();
        if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString(), consentType)) {
            PaymentConsentUtil.populateSinglePaymentBasicConsentData(responseData, requestData);
        } else if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.PERIODIC_PAYMENTS.toString(), consentType)) {
            PaymentConsentUtil.populatePeriodicPaymentBasicConsentData(responseData, requestData);
        } else {
            PaymentConsentUtil.populateBulkPaymentBasicConsentData(responseData, requestData);
        }
    }

    /**
     * Return consumer data for payment consents.
     * This method is responsible for validating the dedicated debtor account from the initiation request and
     * appending it to the consent authorization page.
     *
     * @param responseData data of the response object to populate consent authorize screen for payment consents
     * @param requestData data of the request made to populate consent authorize screen for payment consents
     */
    @Override
    public void populateAccountsData(SuccessResponsePopulateConsentAuthorizeScreenData responseData,
                                     PopulateConsentAuthorizeScreenData requestData)
            throws AuthorizationFailureException, ExtensionException {

        // Extract debtor account reference
        JSONObject debtorAccountRef;
        try {
            JSONObject receipt =
                    new JSONObject(objectMapper.writeValueAsString(requestData.getConsentResource().getReceipt()));
            debtorAccountRef = receipt.getJSONObject(CommonConstants.DEBTOR_ACCOUNT);
        } catch (JsonProcessingException e) {
            throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                    "Failed to fetch consent initiation payload from payment consent", e);
        }

        // Populate subject debtor account of the consent to be authorized by user
        CommonConsentValidationUtil.populateConsentInitiatedAccounts(responseData, requestData, debtorAccountRef);
    }

    /**
     * Returns authorizations and account mappings for account consent persistence.
     * This method is responsible for,
     * <ul>
     *     <li>amending consent status</li>
     *     <li>amending authorization status of the authorized resource</li>
     *     <li>adding account permission mappings to the authorized resource</li>
     * </ul>
     *
     * @param requestBody body of the request received by persist-authorized-consent for payment consents
     * @param authorizingResource authorization resource being authorized for payment consents
     * @return amended payment consent resource
     */
    @Override
    public DetailedConsentResourceDataWithAmendments getAmendedConsentResource(
            PersistAuthorizedConsentRequestBody requestBody, StoredAuthorization authorizingResource)
            throws ExtensionException, AuthorizationFailureException {

        PersistAuthorizedConsent requestData = requestBody.getData();
        boolean isApproved = requestData.getIsApproved();

        // Get auth status from approval
        String authStatus = isApproved ? ExtensionEnums.ScaStatusEnum.FINALISED.toString() :
                ExtensionEnums.ScaStatusEnum.FAILED.toString();

        // Verify that there's only one authorization
        List<AuthorizedResourcesAuthorizedDataInner> authorizedData = requestData.getUserGrantedData()
                .getAuthorizedResources().getAuthorizedData();
        ConsentAuthorizationUtil.verifySingleAuthorizedResource(authorizedData);

        List<Resource> accountMappingResources = AccountConsentUtil.createAccountPermissionMappings(
                requestData.getUserGrantedData().getAuthorizedResources().getAuthorizedData().get(0),
                CommonConstants.DEFAULT_PERMISSION
        );

        // Set new consent status
        return ConsentAuthorizationUtil.buildAmendedConsentResource(requestBody.getRequestId(), authorizingResource,
                isApproved, requestData, accountMappingResources, authStatus);
    }
}
