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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.BulkPaymentInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.PeriodicPaymentInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ScaMethod;
import org.wso2.openbanking.nextgenpsd2.extensions.model.SinglePaymentInitiationPayload;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PopulateConsentAuthorizeScreenData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePopulateConsentAuthorizeScreenData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

/**
 * Utility class for payment consent management.
 */
public class PaymentConsentUtil {
    private static final Log log = LogFactory.getLog(PaymentConsentUtil.class);

    /**
     * Validates payment consent initiation payload based on payment type.
     *
     * @param requestPayload consent initiation request payload
     * @param resourcePath called resource path
     * @throws ValidationFailureException if the payment initiation payload is invalid
     */
    public static void validatePaymentInitiationPayload(JSONObject requestPayload, String resourcePath)
            throws ValidationFailureException, ExtensionException {
        switch (CommonConsentValidationUtil.getServiceDifferentiatingRequestPath(resourcePath)) {
            case CommonConstants.PAYMENTS_SERVICE_PATH:
                CommonConsentValidationUtil
                        .validateJSONFromModel(requestPayload.toString(), SinglePaymentInitiationPayload.class);
                break;

            case CommonConstants.BULK_PAYMENTS_SERVICE_PATH:
                CommonConsentValidationUtil
                        .validateJSONFromModel(requestPayload.toString(), BulkPaymentInitiationPayload.class);
                break;

            case CommonConstants.PERIODIC_PAYMENTS_SERVICE_PATH:
                CommonConsentValidationUtil
                        .validateJSONFromModel(requestPayload.toString(), PeriodicPaymentInitiationPayload.class);
                break;

            default:
                // Execution shouldn't reach here given that path is validated prior to reaching this point
                throw new ValidationFailureException(ValidationFailureException.ErrorCode.INTERNAL_SERVER_ERROR,
                        ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                                TPPMessage.CodeEnum.RESOURCE_UNKNOWN_404, ErrorConstants.PATH_INVALID));
        }
    }

    /**
     * Method to get the payment initiation response without links.
     *
     * @param createdConsent created consent resource data
     * @param scaMethods supported SCA methods
     */
    public static void appendPaymentInitiationResponseToPayload(StoredDetailedConsentResourceData createdConsent,
                                                                ArrayList<ScaMethod> scaMethods, JSONObject payload)
            throws ExtensionException {

        payload.put(CommonConstants.TRANSACTION_STATUS, createdConsent.getStatus());
        payload.put(CommonConstants.PAYMENT_ID, createdConsent.getId());

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
     * Helper method to extract payment product from resource path.
     *
     * @param consentResourcePath payment consent resource path
     * @return payment product
     */
    public static String getPaymentProduct(String consentResourcePath) {
        return consentResourcePath.split("/")[1];
    }

    /**
     * Method to validate payment product for payment consents.
     *
     * @param attributes consent attribute object
     * @param consentResourcePath consent resource path
     * @throws ValidationFailureException if stored payment product does not match one specified in consent resource
     * path
     * @throws ExtensionException is payment product is missing in consent attributes
     */
    public static void validatePaymentProductFromAttributes(Object attributes, String consentResourcePath)
            throws ValidationFailureException, ExtensionException {
        String paymentProductFromPath = getPaymentProduct(consentResourcePath);

        // Extract payment product from attributes
        String paymentProductFromAttributes;
        try {
            JSONObject attributesJSON = CommonConsentValidationUtil.convertObjectToJson(attributes);
            paymentProductFromAttributes = attributesJSON.getString(CommonConstants.PAYMENT_PRODUCT_CC);
        } catch (JSONException e) {
            throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                    "Failed to extract payment product from consent");
        }

        if (!paymentProductFromAttributes.equals(paymentProductFromPath)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(
                    null, TPPMessage.CategoryEnum.ERROR, TPPMessage.CodeEnum.PRODUCT_INVALID,
                    "Consent payment product mismatch"));
        }
    }

    /**
     * Method to populate single payment data into basic consent data.
     *
     * @param responseData the response object to populate
     * @param requestData the request data containing consent receipt
     * @throws ExtensionException if receipt parsing fails
     */
    public static void populateSinglePaymentBasicConsentData(
            SuccessResponsePopulateConsentAuthorizeScreenData responseData,
            PopulateConsentAuthorizeScreenData requestData) throws ExtensionException {

        // Initialize basic consent data
        Map<String, List<String>> basicConsentData = new HashMap<>();

        JSONObject receipt = CommonConsentValidationUtil
                .convertObjectToJson(requestData.getConsentResource().getReceipt());

        // Populate basic consent data with common payment details
        List<String> paymentDetails = new ArrayList<>();
        populateCommonData(receipt, paymentDetails);
        basicConsentData.put(CommonConstants.REQUESTED_DATA_TITLE, paymentDetails);

        // Set basic consent data
        responseData.getConsentData().setBasicConsentData(basicConsentData);
    }

    /**
     * Method to populate bulk payments data into basic consent data.
     *
     * @param responseData the response object to populate
     * @param requestData the request data containing consent receipt
     * @throws ExtensionException in case of invalid data
     */
    public static void populateBulkPaymentBasicConsentData
    (SuccessResponsePopulateConsentAuthorizeScreenData responseData, PopulateConsentAuthorizeScreenData requestData)
            throws ExtensionException {

        // Initialize basic consent data
        Map<String, List<String>> basicConsentData = new HashMap<>();

        JSONObject receipt = CommonConsentValidationUtil
                .convertObjectToJson(requestData.getConsentResource().getReceipt());

        // Build bulk payment array
        JSONArray paymentsArray = receipt.getJSONArray(CommonConstants.PAYMENTS);
        for (int paymentIndex = 0; paymentIndex < paymentsArray.length(); paymentIndex++) {
            JSONObject bulkPayment = paymentsArray.getJSONObject(paymentIndex);
            List<String> consentDataList = new ArrayList<>();

            // Populate common payment data per each payment in bulk payments
            populateCommonData(bulkPayment, consentDataList);

            String title = CommonConstants.PAYMENT_TITLE + (paymentIndex + 1);
            basicConsentData.put(title, consentDataList);
        }

        // Set basic consent data
        responseData.getConsentData().setBasicConsentData(basicConsentData);
    }

    /**
     * Method to populate common payment data for all payment consent types.
     *
     * @param receipt consent initiation payload
     * @param paymentDataList list to which common payment data need be appended
     */
    private static void populateCommonData(JSONObject receipt, List<String> paymentDataList) {

        JSONObject instructedAmount = receipt.getJSONObject(CommonConstants.INSTRUCTED_AMOUNT);

        paymentDataList.add(CommonConstants.INSTRUCTED_AMOUNT_TITLE + ": "
                + instructedAmount.getString(CommonConstants.AMOUNT));
        paymentDataList.add(CommonConstants.INSTRUCTED_CURRENCY_TITLE + ": "
                + instructedAmount.getString(CommonConstants.CURRENCY));

        paymentDataList.add(CommonConstants.CREDITOR_NAME_TITLE + ": "
                + receipt.getString(CommonConstants.CREDITOR_NAME));

        if (StringUtils.isNotBlank(receipt.optString(CommonConstants.CREDITOR_AGENT))) {
            paymentDataList.add(CommonConstants.CREDITOR_AGENT_TITLE + ": "
                    + receipt.getString(CommonConstants.CREDITOR_AGENT));
        }

        JSONObject creditorAccount = receipt.getJSONObject(CommonConstants.CREDITOR_ACCOUNT);

        String creditorAccRefType = CommonConsentValidationUtil.getAccountReferenceType(creditorAccount);
        if (StringUtils.isNotBlank(creditorAccRefType)) {
            paymentDataList.add(String.format(CommonConstants.CREDITOR_REFERENCE_TITLE,
                    creditorAccRefType) + ": " + creditorAccount.getString(creditorAccRefType));
        }

        if (creditorAccount.has(CommonConstants.CURRENCY)
                && StringUtils.isNotBlank(creditorAccount.optString(CommonConstants.CURRENCY))) {
            paymentDataList.add(CommonConstants.CREDITOR_ACCOUNT_CURRENCY_TITLE + ": "
                    + creditorAccount.getString(CommonConstants.CURRENCY));
        }

        if (StringUtils.isNotBlank(receipt.optString(CommonConstants.REMITTANCE_INFO_UNSTRUCTURED))) {
            paymentDataList.add(CommonConstants.REMITTANCE_INFORMATION_UNSTRUCTURED_TITLE + ": "
                    + receipt.getString(CommonConstants.REMITTANCE_INFO_UNSTRUCTURED));
        }

        if (StringUtils.isNotBlank(receipt.optString(CommonConstants.END_TO_END_IDENTIFICATION))) {
            paymentDataList.add(CommonConstants.END_TO_END_IDENTIFICATION_TITLE + ": "
                    + receipt.getString(CommonConstants.END_TO_END_IDENTIFICATION));
        }
    }

    /**
     * Method to populate periodic payment data into basic consent data.
     *
     * @param responseData the response object to populate
     * @param requestData the request data containing consent receipt
     * @throws ExtensionException if receipt parsing fails
     */
    public static void populatePeriodicPaymentBasicConsentData(
            SuccessResponsePopulateConsentAuthorizeScreenData responseData,
            PopulateConsentAuthorizeScreenData requestData) throws ExtensionException {

        // Initialize basic consent data
        Map<String, List<String>> basicConsentData = new HashMap<>();

        JSONObject receipt = CommonConsentValidationUtil
                .convertObjectToJson(requestData.getConsentResource().getReceipt());

        List<String> paymentDetails = new ArrayList<>();

        populateCommonData(receipt, paymentDetails);

        // Append periodic-specific fields
        paymentDetails.add(CommonConstants.START_DATE_TITLE + ": " +
                receipt.optString(CommonConstants.START_DATE));

        if (StringUtils.isNotBlank(receipt.optString(CommonConstants.END_DATE))) {
            paymentDetails.add(CommonConstants.END_DATE_TITLE + ": " +
                    receipt.optString(CommonConstants.END_DATE));
        }

        paymentDetails.add(CommonConstants.FREQUENCY_TITLE + ": " +
                receipt.optString(CommonConstants.FREQUENCY));

        if (StringUtils.isNotBlank(receipt.optString(CommonConstants.EXECUTION_RULE))) {
            paymentDetails.add(CommonConstants.EXECUTION_RULE_TITLE + ": " +
                    receipt.optString(CommonConstants.EXECUTION_RULE));
        }

        basicConsentData.put(CommonConstants.REQUESTED_DATA_TITLE, paymentDetails);

        // Set basic consent data
        responseData.getConsentData().setBasicConsentData(basicConsentData);
    }

    /**
     * Handles payment processing with the banking backend.
     *
     * @param authorizingResource authorized authorization resource
     * @param consentResource authorized consent resource
     */
    public static void handleBackendPayment(StoredAuthorization authorizingResource,
                                            StoredDetailedConsentResourceData consentResource)
            throws ExtensionException {
        String consentType = consentResource.getType();

        if (StringUtils.equals(CommonConstants.PAYMENTS, consentType)
                || StringUtils.equals(CommonConstants.BULK_PAYMENTS, consentType)
                || StringUtils.equals(CommonConstants.PERIODIC_PAYMENTS, consentType)) {
            try {
                String paymentId = consentResource.getId();

                if (StringUtils.equals(ExtensionEnums.AuthTypeEnum.AUTHORISATION.toString(),
                        authorizingResource.getType())
                        && ConsentAuthorizationUtil.areAllOtherAuthResourcesValid(authorizingResource,
                        consentResource.getAuthorizations())) {
                    // If the current authorisation resource is a submission auth resource

                    String paymentReceipt = CommonConsentValidationUtil
                            .convertObjectToJson(consentResource.getReceipt()).toString();
                    if (!ConsentAuthorizationUtil.isPaymentResourceSubmitted(paymentId, paymentReceipt,
                            "submit")) {
                        log.error("Error occurred while submitting the payment," +
                                " please retry");
                        throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                                ErrorConstants.PAYMENT_SUBMISSION_FAILED);
                    }
                } else if (StringUtils.equals(ExtensionEnums.AuthTypeEnum.CANCELLATION.toString(),
                        authorizingResource.getType())
                        && !StringUtils.equals(CommonConstants.PAYMENTS, consentType)
                        && ConsentAuthorizationUtil.areAllOtherAuthResourcesValid(authorizingResource,
                        consentResource.getAuthorizations())) {
                    // If the current authorisation resource is a cancellation auth resource
                    // and consent is not single payment

                    String paymentReceipt = CommonConsentValidationUtil
                            .convertObjectToJson(consentResource.getReceipt()).toString();
                    if (!ConsentAuthorizationUtil.isPaymentResourceSubmitted(paymentId, paymentReceipt,
                            "cancel")) {
                        log.error("Error occurred while cancelling the payment," +
                                " please retry");
                        throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                                ErrorConstants.PAYMENT_CANCELLATION_FAILED);
                    }
                }
            } catch (IOException e) {
                log.error("Exception occurred processing payment, please retry", e);
                throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                        ErrorConstants.PAYMENT_FAILED);
            }
        }
    }
}
