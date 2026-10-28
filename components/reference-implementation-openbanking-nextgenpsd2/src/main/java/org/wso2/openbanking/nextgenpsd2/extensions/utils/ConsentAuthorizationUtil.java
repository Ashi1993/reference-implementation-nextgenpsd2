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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ConfigurationConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.AuthorizationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.AccountReference;
import org.wso2.openbanking.nextgenpsd2.extensions.model.common.Resource;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.Account;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.AmendedAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.AuthorizedResourcesAuthorizedDataInner;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.DetailedConsentResourceDataWithAmendments;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PersistAuthorizedConsent;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Utility class for consent authorization.
 */
public class ConsentAuthorizationUtil {
    private static final Log log = LogFactory.getLog(ConsentAuthorizationUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Validates the consent Id with provided consent type in scope string. If not valid, an error is sent to the
     * redirect URI of the request.
     *
     * @param requestId ID of the request to include in logging
     * @param consentType the consent type
     * @param scopeString the scope string sent in request
     * @throws AuthorizationFailureException thrown if a validation failure happen
     */
    public static void validateConsentTypeWithScopes(String requestId, String consentType, String scopeString)
            throws AuthorizationFailureException {

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating whether the provided consent Id matches with the scope type",
                    requestId));
        }

        if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString(), consentType)
                && !StringUtils.contains(scopeString, CommonConstants.AIS_SCOPE)) {
            log.error(ErrorConstants.CONSENT_ID_AND_SCOPE_MISMATCH);
            throw new AuthorizationFailureException(ErrorConstants.CONSENT_ID_AND_SCOPE_MISMATCH);
        }

        if ((StringUtils.equals(ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString(), consentType)
                || StringUtils.equals(ExtensionEnums.ConsentTypeEnum.BULK_PAYMENTS.toString(), consentType)
                || StringUtils.equals(ExtensionEnums.ConsentTypeEnum.PERIODIC_PAYMENTS.toString(), consentType))
                && !StringUtils.contains(scopeString, CommonConstants.PIS_SCOPE)) {
            log.error(ErrorConstants.CONSENT_ID_AND_SCOPE_MISMATCH);
            throw new AuthorizationFailureException(ErrorConstants.CONSENT_ID_AND_SCOPE_MISMATCH);
        }

        if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString(), consentType)
                && !StringUtils.contains(scopeString, CommonConstants.PIIS_SCOPE)) {
            log.error(ErrorConstants.CONSENT_ID_AND_SCOPE_MISMATCH);
            throw new AuthorizationFailureException(ErrorConstants.CONSENT_ID_AND_SCOPE_MISMATCH);
        }
    }

    /**
     * Checks all authorization resources to see if there's any unauthorized resources unbound to a user or bound to
     * this user.
     *
     * @param requestId ID of the request to include in logging
     * @param authorizations authorization objects from request body
     * @param userId ID of the authorizing user
     * @param consentResource stored consent resource retrieved from accelerator
     */
    public static StoredAuthorization getAuthorizableResource(String requestId,
                                                              List<StoredAuthorization> authorizations, String userId,
                                                              StoredDetailedConsentResourceData consentResource)
            throws AuthorizationFailureException {
        String authType;
        if (StringUtils.equals(consentResource.getStatus(), ExtensionEnums.TransactionStatusEnum.ACTC.name())) {
            authType = ExtensionEnums.AuthTypeEnum.CANCELLATION.toString();
        } else {
            authType = ExtensionEnums.AuthTypeEnum.AUTHORISATION.toString();
        }

        // Filter by auth type and status
        // Checks all auth resources to see if any are bound the user
        StoredAuthorization authorizableAuthObj = null;
        String userIdFromAuthObj;
        for (StoredAuthorization authObj : authorizations) {
            userIdFromAuthObj = authObj.getUserId();
            if (StringUtils.equals(userIdFromAuthObj, userId)) {
                if (authType.equals(authObj.getType()) &&
                        !ExtensionEnums.ScaStatusEnum.FINALISED.toString().equals(authObj.getStatus()) &&
                        !ExtensionEnums.ScaStatusEnum.EXEMPTED.toString().equals(authObj.getStatus())) {
                    // Validate consent status
                    validateConsentStatus(requestId, consentResource, authObj);
                    return authObj;
                }

                // If user has authorized once, user has no more authorizable resources
                break;
            }

            if (authType.equals(authObj.getType()) &&
                    !ExtensionEnums.ScaStatusEnum.FINALISED.toString().equals(authObj.getStatus()) &&
                    !ExtensionEnums.ScaStatusEnum.EXEMPTED.toString().equals(authObj.getStatus()) &&
                    userIdFromAuthObj == null) {
                // Validate consent status
                validateConsentStatus(requestId, consentResource, authObj);
                authorizableAuthObj = authObj;
            }
        }

        if (authorizableAuthObj != null) {
            return authorizableAuthObj;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Valid unauthenticated authorization not found for Consent Id %s%s", requestId,
                    consentResource.getId(), ((userId == null) ? "" : " for given PSU of Id: " + userId)));
        }
        throw new AuthorizationFailureException("An unauthenticated authorization is not found for this consent");
    }

    /**
     * Validates authorization status based on consent type.
     *
     * @param requestId ID of the request to include in logging
     * @param consentResource consent resource from the request
     * @param authObj authorization object to authorize
     */
    private static void validateConsentStatus(String requestId, StoredDetailedConsentResourceData consentResource,
                                              StoredAuthorization authObj) throws AuthorizationFailureException {
        String consentStatus = consentResource.getStatus();
        String type = consentResource.getType();
        boolean isApplicable = false;

        if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString(), type)) {
            isApplicable = StringUtils.equals(ExtensionEnums.ConsentStatusEnum.RECEIVED.toString(), consentStatus)
                    || StringUtils.equals(ExtensionEnums.ConsentStatusEnum.PARTIALLY_AUTHORISED.toString(),
                    consentStatus);

        } else if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString(), type)
                || StringUtils.equals(ExtensionEnums.ConsentTypeEnum.BULK_PAYMENTS.toString(), type)
                || StringUtils.equals(ExtensionEnums.ConsentTypeEnum.PERIODIC_PAYMENTS.toString(), type)) {
            if (StringUtils.equals(ExtensionEnums.AuthTypeEnum.CANCELLATION.toString(), authObj.getType())) {
                // Ignores consent status
                isApplicable = true;
            } else {
                isApplicable = StringUtils.equals(ExtensionEnums.TransactionStatusEnum.RCVD.name(), consentStatus)
                        || StringUtils.equals(ExtensionEnums.TransactionStatusEnum.PATC.name(), consentStatus);
            }

        } else if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString(), type)) {
            isApplicable = StringUtils.equals(ExtensionEnums.ConsentStatusEnum.RECEIVED.toString(), consentStatus)
                    || StringUtils.equals(ExtensionEnums.ConsentStatusEnum.PARTIALLY_AUTHORISED.toString(),
                    consentStatus);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] The consent with Id: %s is in %s status. It is %s to authorize", requestId,
                    consentResource.getId(), consentStatus, isApplicable ? "applicable" : "not applicable"));
        }

        if (!isApplicable) {
            log.error(String.format("[%s] The consent of Id: %s is not in an applicable status for authorization",
                    requestId, consentResource.getId()));
            throw new AuthorizationFailureException("The consent is not in an applicable status for authorization");
        }
    }

    /**
     * Retrieves all the accounts for a particular account number in multi-currency scenarios.
     *
     * @param accountRefObject single account json object
     * @param accountRefArray     all the accounts json array
     * @return accounts array
     */
    public static JSONArray getFilteredAccountsForAccountNumber(JSONObject accountRefObject,
                                                                JSONArray accountRefArray) {
        String accountRefType = CommonConsentValidationUtil.getAccountReferenceType(accountRefObject);
        String accountNumber = accountRefObject.getString(accountRefType);
        JSONArray filteredAccountRefObjects = new JSONArray();

        // Filtering the accounts with the same account number
        for (Object accountObject : accountRefArray) {
            JSONObject accountJSON = (JSONObject) accountObject;
            if (StringUtils.equals(accountJSON.optString(accountRefType), accountNumber)) {
                filteredAccountRefObjects.put(accountObject);
            }
        }

        return filteredAccountRefObjects;
    }

    /**
     * Builds account objects for consent page based on account ref objects.
     *
     * @param accountRefJSON account reference JSON object
     * @return built account object to include in populate-consent-authorize-screen response
     */
    public static Account getAccountFromAccountRef(JSONObject accountRefJSON) {
        String refType = CommonConsentValidationUtil.getAccountReferenceType(accountRefJSON);
        String accountRef = accountRefJSON.getString(refType);
        Account accountObject = new Account();
        accountObject.setDisplayName(refType + " " + accountRef);
        accountObject.setAdditionalProperty(refType, accountRef);

        // Add currency if exists
        String accountCurrency = accountRefJSON.optString(CommonConstants.CURRENCY);
        if (accountCurrency != null && !accountCurrency.isEmpty()) {
            accountObject.setDisplayName(accountObject.getDisplayName() + " (" + accountCurrency + ")");
            accountObject.setAdditionalProperty(CommonConstants.CURRENCY, accountCurrency);
        }
        return accountObject;
    }

    /**
     * Validates requested accounts based on retrieved accounts from the banking backend.
     *
     * @param accountRefList list of account references
     * @param accountList list of account objects from banking backend
     * @return list of validated account objects
     */
    public static List<Account> getValidatedAccountObjects(JSONArray accountRefList, JSONArray accountList) {
        List<Account> validatedAccountObjects = new ArrayList<>();
        for (Object accountObject : accountRefList) {
            JSONObject accountRefObject = (JSONObject) accountObject;

            // Build account object for account reference object
            Account accountObj = getAccountFromAccountRef(accountRefObject);

            // Skipping validation for maskedPan based account reference types and this needs to be validated
            // from the bank back end since there might be scenarios where there are 2 similar maskedPans
            // for a single user therefore we are not sure which account to validate it against
            // Eg: 123456xxxxxx1234, 123456xxxxxx1234 -> Both these maskedPans can belong to the same user
            String accountRefType = CommonConsentValidationUtil.getAccountReferenceType(accountRefObject);
            if (StringUtils.equals(accountRefType, CommonConstants.MASKED_PAN)) {
                validatedAccountObjects.add(accountObj);
                continue;
            }

            JSONArray filteredAccountRefObjects =
                    getFilteredAccountsForAccountNumber(accountRefObject, accountList);

            if (filteredAccountRefObjects.length() > 1) {
                // Multi currency account
                if (accountRefObject.has(CommonConstants.CURRENCY)) {
                    for (Object object : filteredAccountRefObjects) {
                        JSONObject filteredAccountRefObject = (JSONObject) object;
                        if (filteredAccountRefObject.getString(CommonConstants.CURRENCY)
                                .equalsIgnoreCase(accountRefObject.getString(CommonConstants.CURRENCY))) {
                            validatedAccountObjects.add(accountObj);
                            break;
                        }
                    }
                } else {
                    // Adding all the accounts when TPP initiated a multi-currency account
                    // without specifying the currency
                    for (Object object: filteredAccountRefObjects) {
                        validatedAccountObjects
                                .add(getAccountFromAccountRef((JSONObject) object));
                    }
                }
            } else if (filteredAccountRefObjects.length() == 1) {
                if (!accountRefObject.has(CommonConstants.CURRENCY)) {
                    validatedAccountObjects.add(accountObj);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        return validatedAccountObjects;
    }

    /**
     * Validates requested accounts based on retrieved accounts from the banking backend.
     *
     * @param accountRefList list of account references
     * @param accountList list of account objects from banking backend
     * @return list of validated account objects
     */
    public static List<Account> getValidatedAccountObjects(List<AccountReference> accountRefList,
                                                    JSONArray accountList) throws ExtensionException {
        try {
            JSONArray accountRefArrayJSON = new JSONArray(objectMapper.writeValueAsString(accountRefList));
            return getValidatedAccountObjects(accountRefArrayJSON, accountList);
        } catch (JsonProcessingException e) {
            throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                    "Failed to process account reference objects", e);
        }
    }

    /**
     * This method checks whether all the authorization resources are authorized. There are two scenarios.
     * <p>
     * 1. There can be only one authorization resource for a consent.
     * <p>
     * In this scenario, this method will return true if the current authorization resource get the approval to update
     * to psuAuthenticated status. After the payment submission is successfully done, the BerlinConsentPersistStep will
     * do the real update of the authorization resource status. Otherwise, false will be returned to indicate that the
     * authorization resource of this consent is not in psuAuthenticated state. Therefore, the payment
     * submission/cancellation will not happen.
     * <p>
     * 2. In multi level scenario, there can be multiple authorization resources per consent.
     * <p>
     * In this scenario, this method will return true if all other authorization resources except current one are in
     * psuAuthenticated status. After the payment submission is successfully done, the BerlinConsentPersistStep will
     * do the real updating of the authorization resource status. Otherwise, false will be returned to indicate that
     * all the authorization resources related to the current consent are not in psuAuthenticated status. Therefore,
     * the payment submission/cancellation will not happen.
     *
     * @param authorizingResource current authorization resource to be authorized
     * @param allAuthorizations all the authorizations associated with this consent
     * @return true or false according to the aforementioned scenarios
     */
    public static boolean areAllOtherAuthResourcesValid(StoredAuthorization authorizingResource,
                                                        List<StoredAuthorization> allAuthorizations) {
        //ToDo: Make sure in explicit consent authorization that only one authorization can be created per user
        //Remove current authorization resource from the list
        allAuthorizations.removeIf(resource
                -> (StringUtils.equals(resource.getId(), authorizingResource.getId())
                || !StringUtils.equals(resource.getType(), authorizingResource.getType())));

        if (allAuthorizations.isEmpty()) {
            return true;
        } else {
            return allAuthorizations.stream().allMatch(authorisation
                    -> StringUtils.equals(authorisation.getStatus(),
                    ExtensionEnums.ScaStatusEnum.PSU_AUTHENTICATED.toString()));
        }
    }

    /**
     * This method contains the http client implementation to send the POST request to submit the payment to the bank.
     * The parameter "submissionType" determines whether the payment resource is submitted for the real payment to
     * happen or for the payment cancellation.
     *
     * @param paymentId ID of the payment to submit/cancel
     * @param paymentData payment data to be submitted to the backend
     * @param submissionType the submission type (payment submission or cancellation)
     * @return true if submission is a success, false otherwise
     * @throws IOException thrown if an error occurs executing the request
     */
    public static boolean isPaymentResourceSubmitted(String paymentId, String paymentData, String submissionType)
            throws IOException {

        String paymentBackendURL = ConfigurationConstants.PAYMENT_BACKEND_URL;

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(paymentBackendURL + "/" + submissionType + "/" + paymentId);

            // Set headers
            request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");

            // Set body
            StringEntity stringEntity = new StringEntity(paymentData);
            request.setEntity(stringEntity);

            HttpResponse response = client.execute(request);

            return (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_ACCEPTED);
        }
    }

    /**
     * Builds detailed consent resource with new consent status and amended authorizations.
     *
     * @param requestData data sent with the persist-authorized-consent request
     * @param newConsentStatus updated consent status
     * @param amendedAuthorization authorization resources being amended (authorized with account mappings)
     * @return data for the response sent to persist-authorized-consent request
     */
    public static DetailedConsentResourceDataWithAmendments buildDetailedConsentResourceDataWithAmendments(
            PersistAuthorizedConsent requestData, String newConsentStatus, AmendedAuthorization amendedAuthorization) {
        StoredDetailedConsentResourceData storedConsentData = requestData.getConsentResource();
        DetailedConsentResourceDataWithAmendments amendedConsent = new DetailedConsentResourceDataWithAmendments();
        amendedConsent.setType(storedConsentData.getType());
        amendedConsent.setStatus(newConsentStatus);
        amendedConsent.setValidityTime((long) storedConsentData.getValidityTime());
        amendedConsent.setRecurringIndicator(storedConsentData.getRecurringIndicator());
        amendedConsent.setFrequency(storedConsentData.getFrequency());
        amendedConsent.setReceipt(storedConsentData.getReceipt());
        amendedConsent.setAmendments(Collections.singletonList(amendedAuthorization));
        return amendedConsent;
    }

    /**
     * Calculates aggregate consent status based on status of all authorization resources.
     *
     * @param storedAuthorizations authorization resources sent with the request
     * @param authorizingResource resource being authorized by the SCA
     * @return new consent status based on authorization status of all authorization objects
     */
    public static Optional<String> getAggregatedConsentStatus(List<StoredAuthorization> storedAuthorizations,
                                                                         StoredAuthorization authorizingResource,
                                                                         boolean isTransaction) {

        // Have all authorisations passed.
        boolean hasPassed = storedAuthorizations.stream().allMatch(
                authorisation -> authorisation.getStatus().equals(
                        ExtensionEnums.ScaStatusEnum.FINALISED.toString())
                        || StringUtils.equals(authorisation.getId(), authorizingResource.getId()));

        if (hasPassed) {
            if (isTransaction) {
                return Optional.of(ExtensionEnums.TransactionStatusEnum.ACCP.name());
            } else {
                return Optional.of(ExtensionEnums.ConsentStatusEnum.VALID.toString());
            }
        }

        // Has at least one authorisation failed
        boolean hasFailed = storedAuthorizations.stream().anyMatch(
                authorisation -> authorisation.getStatus().equals(
                        ExtensionEnums.ScaStatusEnum.FAILED.toString())
                        || StringUtils.equals(authorisation.getId(), authorizingResource.getId()));

        if (hasFailed) {
            if (isTransaction) {
                return Optional.of(ExtensionEnums.TransactionStatusEnum.RJCT.name());
            } else {
                return Optional.of(ExtensionEnums.ConsentStatusEnum.REJECTED.toString());
            }
        }

        // Has at least a single successful authorisation taken place.
        boolean partiallyPassed = storedAuthorizations.stream().anyMatch(authorisation ->
                authorisation.getStatus().equals(ExtensionEnums.ScaStatusEnum.FINALISED.toString()));

        if (partiallyPassed) {
            if (isTransaction) {
                return Optional.of(ExtensionEnums.TransactionStatusEnum.PATC.name());
            } else {
                return Optional.of(ExtensionEnums.ConsentStatusEnum.PARTIALLY_AUTHORISED.toString());
            }
        }
        return Optional.empty();
    }

    /**
     * Builds amended authorization object to be forwarded as response to persist-authorized-consent request.
     *
     * @param authorizingResource resource being authorized
     * @param accountMappingResources account mappings
     * @param authStatus updated authorization status
     * @return amended authorization object
     */
    public static AmendedAuthorization buildAmendedAuthorization(StoredAuthorization authorizingResource,
                                                                 List<Resource> accountMappingResources,
                                                                 String authStatus) {
        AmendedAuthorization amendedAuthorization = new AmendedAuthorization();
        amendedAuthorization.setId(authorizingResource.getId());
        amendedAuthorization.setResources(accountMappingResources);
        amendedAuthorization.setType(authorizingResource.getType());
        amendedAuthorization.setStatus(authStatus);
        return amendedAuthorization;
    }

    /**
     * Verify that only a single account was authorized for this consent.
     * @param authorizedData data authorized through the consent authorization user input
     */
    public static void verifySingleAuthorizedResource(List<AuthorizedResourcesAuthorizedDataInner> authorizedData)
            throws ExtensionException {
        if (authorizedData.size() > 1 || authorizedData.get(0).getAccounts().size() > 1) {
            log.error("Retrieved more than one authorized account for a consent that " +
            "can have only a single account");
            throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                    "Retrieved more than one authorized account for a consent that " +
                    "can have only a single account");
        }
    }

    /**
     * Method to build consent amendment resource to forward back to the accelerator.
     *
     * @param requestId ID of the request to include in logging
     * @param authorizingResource authorization resource being authorized
     * @param isApproved whether the consent was approved or not
     * @param requestData request data retrieved at the persist-authorized-consent endpoint
     * @param accountMappingResources list of account to permission mapping resources
     * @param authStatus new status of the authorization resource
     * @return built consent amendment resource
     * @throws ExtensionException if consent was approved without selecting accounts
     */
    public static DetailedConsentResourceDataWithAmendments buildAmendedConsentResource(String requestId,
            StoredAuthorization authorizingResource, boolean isApproved, PersistAuthorizedConsent requestData,
            List<Resource> accountMappingResources, String authStatus)
            throws ExtensionException, AuthorizationFailureException {

        String consentType = requestData.getConsentResource().getType();
        boolean isTransaction = ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString().equals(consentType)
                || ExtensionEnums.ConsentTypeEnum.PERIODIC_PAYMENTS.toString().equals(consentType)
                || ExtensionEnums.ConsentTypeEnum.BULK_PAYMENTS.toString().equals(consentType);

        String newConsentStatus;
        if (!isApproved) {
            newConsentStatus = (isTransaction) ? ExtensionEnums.TransactionStatusEnum.RJCT.name() :
                    ExtensionEnums.ConsentStatusEnum.REJECTED.toString();
        } else {
            if (accountMappingResources.isEmpty()) {
                if (ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString().equals(consentType)) {
                    // Approved with no account selections
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("[%s] %s", requestId, ErrorConstants.APPROVE_WITH_NO_ACCOUNTS_ERROR));
                    }
                    throw new AuthorizationFailureException(ErrorConstants.APPROVE_WITH_NO_ACCOUNTS_ERROR);
                } else {
                    log.error(ErrorConstants.APPROVE_WITH_NO_ACCOUNTS_ERROR);
                    throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request",
                            ErrorConstants.APPROVE_WITH_NO_ACCOUNTS_ERROR);
                }
            }

            // Set aggregate consent status
            Optional<String> consentStatus = getAggregatedConsentStatus(
                    requestData.getConsentResource().getAuthorizations(),
                    authorizingResource,
                    isTransaction
            );

            if (consentStatus.isPresent()) {
                newConsentStatus = consentStatus.get();
            } else {
                log.error(String.format(ErrorConstants.INVALID_CONSENT_STATUS_UPDATE,
                        requestData.getConsentId()));
                throw new ExtensionException(Response.Status.BAD_REQUEST, "invalid_request", 
                        String.format(ErrorConstants.INVALID_CONSENT_STATUS_UPDATE,
                                requestData.getConsentId()));
            }
        }

        // Build auth amendments
        AmendedAuthorization amendedAuthorization =
                buildAmendedAuthorization(authorizingResource, accountMappingResources,
                        authStatus);

        // Build and return amended consent
        return buildDetailedConsentResourceDataWithAmendments(requestData, newConsentStatus,
                amendedAuthorization);
    }
}
