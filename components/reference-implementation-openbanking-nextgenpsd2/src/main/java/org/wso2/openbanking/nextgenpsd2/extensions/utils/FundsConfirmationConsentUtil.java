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

package org.wso2.openbanking.nextgenpsd2.extensions.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ScaMethod;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PopulateConsentAuthorizeScreenData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePopulateConsentAuthorizeScreenData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for payment consent management.
 */
public class FundsConfirmationConsentUtil {

    /**
     * Method to get the funds confirmation initiation response without links.
     *
     * @param createdConsent created consent retrieved
     * @param scaMethods supported sca methods
     * @param payload consent initiation payload
     */
    public static void appendCoFInitiationResponseToPayload(StoredDetailedConsentResourceData createdConsent,
                                                            ArrayList<ScaMethod> scaMethods, JSONObject payload)
            throws ExtensionException {
        payload.put(CommonConstants.CONSENT_STATUS, createdConsent.getStatus());
        payload.put(CommonConstants.CONSENT_ID, createdConsent.getId());

        JSONArray chosenSCAMethods = new JSONArray();
        for (ScaMethod scaMethod : scaMethods) {
            chosenSCAMethods.put(CommonConsentValidationUtil.convertObjectToJson(scaMethod));
        }

        if (scaMethods.size() > 1) {
            payload.put(CommonConstants.SCA_METHODS, chosenSCAMethods);
        } else {
            payload.put(CommonConstants.CHOSEN_SCA_METHOD, chosenSCAMethods.get(0));
        }
    }

    /**
     * Method to populate funds confirmation data into basic consent data.
     *
     * @param responseData the response object to populate
     * @param requestData the request data containing consent receipt
     * @throws ExtensionException if receipt parsing fails
     */
    public static void populateFundsConfirmationBasicConsentData
    (SuccessResponsePopulateConsentAuthorizeScreenData responseData, PopulateConsentAuthorizeScreenData requestData)
            throws ExtensionException {

        Map<String, List<String>> basicConsentData = new HashMap<>();

        JSONObject receipt = CommonConsentValidationUtil
                .convertObjectToJson(requestData.getConsentResource().getReceipt());

        List<String> consentDetails = new ArrayList<>();

        // Card number
        if (receipt.has(CommonConstants.CARD_NUMBER) &&
                StringUtils.isNotBlank(receipt.optString(CommonConstants.CARD_NUMBER))) {
            consentDetails.add(CommonConstants.CARD_NUMBER_TITLE + ": " +
                    receipt.optString(CommonConstants.CARD_NUMBER));
        }

        // Card expiry date
        if (receipt.has(CommonConstants.CARD_EXPIRY_DATE) &&
                StringUtils.isNotBlank(receipt.optString(CommonConstants.CARD_EXPIRY_DATE))) {
            consentDetails.add(CommonConstants.CARD_EXPIRY_DATE_TITLE + ": " +
                    receipt.optString(CommonConstants.CARD_EXPIRY_DATE));
        }

        // Card information
        if (receipt.has(CommonConstants.CARD_INFORMATION) &&
                StringUtils.isNotBlank(receipt.optString(CommonConstants.CARD_INFORMATION))) {
            consentDetails.add(CommonConstants.CARD_INFORMATION_TITLE + ": " +
                    receipt.optString(CommonConstants.CARD_INFORMATION));
        }

        basicConsentData.put(CommonConstants.CONSENT_DETAILS_TITLE, consentDetails);

        responseData.getConsentData().setBasicConsentData(basicConsentData);
    }
}
