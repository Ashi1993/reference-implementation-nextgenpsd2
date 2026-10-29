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

package org.wso2.openbanking.nextgenpsd2.extensions.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ConfigurationConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ConsentAccessContext;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for validating data access against a stored consent.
 */
public class ConsentValidationUtil {

    private static final Log log = LogFactory.getLog(ConsentValidationUtil.class);


    /**
     * Flattens the resources of every authorisation of the consent into a single list.
     * These are the equivalent of the consent mapping resources of the accelerator.
     *
     * @param consentResource the stored consent resource
     * @return every resource mapped to the consent
     */
    public static List<StoredResource> getStoredResources(StoredDetailedConsentResourceData consentResource) {

        List<StoredResource> resources = new ArrayList<>();
        if (consentResource == null || consentResource.getAuthorizations() == null) {
            return resources;
        }
        for (StoredAuthorization authorization : consentResource.getAuthorizations()) {
            if (authorization.getResources() != null) {
                resources.addAll(authorization.getResources());
            }
        }
        return resources;
    }

    /**
     * Checks if there is any active resource mapped to the consent.
     *
     * @param resources resources mapped to the consent
     * @return true if there is at least one active resource
     */
    public static boolean hasAnyActiveMappingResource(List<StoredResource> resources) {

        for (StoredResource resource : resources) {
            if (StringUtils.equals(resource.getStatus(), CommonConstants.ACTIVE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there is an active resource that matches the provided permission. This method should be used when
     * account ID validation is disabled. It does not check whether the account ID is present in the resource, it
     * only checks whether the permission is present for the given consent.
     *
     * @param accessMethod access method requested
     * @param resources    resources mapped to the consent
     * @return true if a resource matches the access method
     */
    public static boolean hasValidPermissionsForConsentId(String accessMethod, List<StoredResource> resources) {

        for (StoredResource resource : resources) {
            if (StringUtils.equals(resource.getPermission(), accessMethod)
                    && StringUtils.equals(resource.getStatus(), CommonConstants.ACTIVE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there is an active resource that matches both the provided account ID and permission. This method
     * should be used when account ID validation is enabled. Card accounts are skipped since those references are
     * validated by the bank.
     *
     * @param accountId    account ID requested
     * @param accessMethod access method requested
     * @param resources    resources mapped to the consent
     * @return true if a resource matches the account ID and access method combination
     */
    public static boolean hasValidPermissionsForAccountId(String accountId, String accessMethod,
                                                          List<StoredResource> resources) {

        for (StoredResource resource : resources) {
            String accountReference = resource.getAccountId();
            if (accountReference == null || isCardAccountReference(accountReference)) {
                continue;
            }
            if (StringUtils.equals(resource.getPermission(), accessMethod)
                    && accountReference.contains(accountId)
                    && StringUtils.equals(resource.getStatus(), CommonConstants.ACTIVE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a stored account reference belongs to a card account.
     *
     * @param accountReference stored account reference
     * @return true if the reference is a card account reference
     */
    public static boolean isCardAccountReference(String accountReference) {

        return StringUtils.contains(accountReference, CommonConstants.MASKED_PAN)
                || StringUtils.contains(accountReference, CommonConstants.PAN);
    }

    /**
     * Checks whether the request addresses a single account, such as /accounts/{account-id}/balances.
     *
     * @param resourcePath path of the resource being accessed
     * @return true if the request is a single account retrieval
     */
    public static boolean isSingleAccountRetrieveRequest(String resourcePath) {

        return matchesAny(resourcePath, CommonConstants.SINGLE_ACCOUNT_ACCESS_METHODS_REGEX_LIST);
    }

    /**
     * Checks whether the request addresses the full list of accounts, such as /accounts.
     *
     * @param resourcePath path of the resource being accessed
     * @return true if the request is a bulk account retrieval
     */
    public static boolean isBulkAccountRetrieveRequest(String resourcePath) {

        return matchesAny(resourcePath, CommonConstants.BULK_ACCOUNT_ACCESS_METHODS_REGEX_LIST);
    }

    /**
     * Extracts the account ID addressed by the request from its path.
     *
     * @param pathElements elements of the resource path
     * @return the account ID, or an empty string if the path does not address one
     */
    public static String getAccountIdFromPath(List<String> pathElements) {

        int accountsIndex = -1;
        if (pathElements.contains(ExtensionEnums.AccessMethodEnum.ACCOUNTS.toString())) {
            accountsIndex = pathElements.indexOf(ExtensionEnums.AccessMethodEnum.ACCOUNTS.toString());
        } else if (pathElements.contains(CommonConstants.CARD_ACCOUNTS)) {
            accountsIndex = pathElements.indexOf(CommonConstants.CARD_ACCOUNTS);
        }

        if (accountsIndex == -1 || accountsIndex + 1 >= pathElements.size()
                || pathElements.get(accountsIndex + 1).isEmpty()) {
            return "";
        }
        return pathElements.get(accountsIndex + 1).split("\\?")[0];
    }

    /**
     * Determines the access method the request needs from its path. A path that addresses neither balances nor
     * transactions needs the accounts access method.
     *
     * @param pathElements elements of the resource path
     * @return the access method required by the request
     */
    public static String getAccessMethodFromPath(List<String> pathElements) {

        for (String pathElement : pathElements) {
            if (pathElement.contains(ExtensionEnums.AccessMethodEnum.BALANCES.toString())) {
                return ExtensionEnums.AccessMethodEnum.BALANCES.toString();
            }
            if (pathElement.contains(ExtensionEnums.AccessMethodEnum.TRANSACTIONS.toString())) {
                return ExtensionEnums.AccessMethodEnum.TRANSACTIONS.toString();
            }
        }
        return ExtensionEnums.AccessMethodEnum.ACCOUNTS.toString();
    }

    /**
     * Extracts the payment ID addressed by the request from its path. The payment ID is the last element of the
     * path, except on a status request where it is the element before last.
     *
     * @param resourcePath path of the resource being accessed
     * @return the payment ID addressed by the request
     */
    public static String getPaymentIdFromPath(String resourcePath) {

        List<String> pathElements = splitPath(resourcePath);
        if (pathElements.isEmpty()) {
            return "";
        }
        if (resourcePath.endsWith(CommonConstants.STATUS) && pathElements.size() > 1) {
            return pathElements.get(pathElements.size() - 2);
        }
        return pathElements.get(pathElements.size() - 1);
    }

    /**
     * Splits a resource path into its elements, ignoring the leading separator.
     *
     * @param resourcePath path of the resource being accessed
     * @return elements of the path
     */
    public static List<String> splitPath(String resourcePath) {

        if (StringUtils.isBlank(resourcePath)) {
            return new ArrayList<>();
        }
        return Arrays.asList(StringUtils.stripStart(resourcePath, "/").split("/"));
    }

    /**
     * Returns the first element of a resource path, which identifies the service the request belongs to.
     *
     * @param resourcePath path of the resource being accessed
     * @return the first element of the path, or an empty string if there is none
     */
    public static String getServicePathIdentifier(String resourcePath) {

        List<String> pathElements = splitPath(resourcePath);
        return pathElements.isEmpty() ? "" : pathElements.get(0).split("\\?")[0];
    }

    private static boolean matchesAny(String resourcePath, List<String> patterns) {

        if (StringUtils.isBlank(resourcePath)) {
            return false;
        }
        String pathToMatch = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        for (String pattern : patterns) {
            if (pathToMatch.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the account reference in the format it is stored against the consent, which is the account reference
     * type, the reference and the currency if present, separated by the delimiter.
     *
     * @param accountRefObject account reference object from the request payload
     * @return account reference in its stored format, or an empty string if the reference type is not supported
     */
    public static String getAccountReferenceToPersist(JSONObject accountRefObject) {

        String referenceType = CommonConsentValidationUtil.getAccountReferenceType(accountRefObject);
        if (referenceType == null) {
            return "";
        }
        String accountReference = String.format("%s%s%s", referenceType, CommonConstants.DELIMITER,
                accountRefObject.getString(referenceType));
        if (accountRefObject.has(CommonConstants.CURRENCY)) {
            accountReference += String.format("%s%s", CommonConstants.DELIMITER,
                    accountRefObject.getString(CommonConstants.CURRENCY));
        }
        return accountReference;
    }

    /**
     * Checks whether the provided account reference is mapped to an active resource of the consent. Masked PAN
     * references are skipped since those must be validated by the bank.
     *
     * @param accountRefObject account reference object from the request payload
     * @param resource         resource mapped to the consent
     * @return true if the account reference matches the resource
     */
    public static boolean isAccountReferenceMapped(JSONObject accountRefObject, StoredResource resource) {

        if (!StringUtils.equals(resource.getStatus(), CommonConstants.ACTIVE)) {
            return false;
        }
        String mappedAccountId = resource.getAccountId();
        if (StringUtils.contains(mappedAccountId, CommonConstants.MASKED_PAN)) {
            return true;
        }
        return StringUtils.equals(mappedAccountId, getAccountReferenceToPersist(accountRefObject));
    }

    /**
     * Validates the context of the data access request, which is the part of the request the accelerator sends
     * beyond the declared schema of the extension point. A deployment that sends the declared payload only skips
     * these validations unless they are configured as required.
     *
     * @param requestId       correlation identifier of the request
     * @param consentResource the stored consent resource
     * @param context         context of the data access request
     * @throws ValidationFailureException if the data access request context is invalid
     * @throws ExtensionException if construction of an error failed
     */
    public static void validateDataRequestContext(String requestId,
                                                  StoredDetailedConsentResourceData consentResource,
                                                  ConsentAccessContext context)
            throws ValidationFailureException, ExtensionException {

        if (!context.isPresent()) {
            if (ConfigurationConstants.REQUIRE_CONSENT_ACCESS_CONTEXT) {
                throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                        ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                                TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.DATA_REQUEST_CONTEXT_MISSING));
            }
            log.warn(String.format("[%s] The data access request carries no context, so the header, client and "
                    + "user validations are skipped", requestId));
            return;
        }

        validateRequestHeaders(consentResource, context);
        validateTokenBinding(consentResource, context);
    }

    /**
     * Validates the headers of the data access request. The Consent-ID header is not sent on payment requests,
     * where the payment is addressed by the path instead.
     *
     * @param consentResource the stored consent resource
     * @param context         context of the data access request
     * @throws ValidationFailureException if a header is missing or malformed
     * @throws ExtensionException if construction of an error failed
     */
    private static void validateRequestHeaders(StoredDetailedConsentResourceData consentResource,
                                               ConsentAccessContext context)
            throws ValidationFailureException, ExtensionException {

        if (!context.hasHeaders()) {
            return;
        }

        String xRequestId = context.getHeader(CommonConstants.X_REQUEST_ID_PROPER_CASE_HEADER);
        if (StringUtils.isBlank(xRequestId)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.X_REQUEST_ID_MISSING));
        }
        if (!CommonConsentValidationUtil.isValidUuid(xRequestId)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.X_REQUEST_ID_INVALID));
        }

        // The PSU-IP-Address header is optional on a data access request, but must carry a value when sent
        String psuIpAddress = context.getHeader(CommonConstants.PSU_IP_ADDRESS_PROPER_CASE_HEADER);
        if (psuIpAddress != null && StringUtils.isBlank(psuIpAddress)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, String.format("Invalid %s header",
                                    CommonConstants.PSU_IP_ADDRESS_PROPER_CASE_HEADER)));
        }

        if (isPaymentConsent(consentResource.getType())) {
            return;
        }

        String consentIdHeader = context.getHeader(CommonConstants.CONSENT_ID_PROPER_CASE_HEADER);
        if (StringUtils.isBlank(consentIdHeader)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.CONSENT_ID_MISSING));
        }
        if (!CommonConsentValidationUtil.isValidUuid(consentIdHeader)) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.CONSENT_ID_INVALID));
        }
        if (!StringUtils.equals(consentIdHeader, consentResource.getId())) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.NOT_FOUND,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.RESOURCE_UNKNOWN_404, ErrorConstants.NO_CONSENT_FOR_CLIENT_ERROR));
        }
    }

    /**
     * Validates that the token the data access request was made with belongs to the client and the user of the
     * consent being accessed.
     *
     * @param consentResource the stored consent resource
     * @param context         context of the data access request
     * @throws ValidationFailureException if the token does not belong to the consent
     * @throws ExtensionException if construction of an error failed
     */
    private static void validateTokenBinding(StoredDetailedConsentResourceData consentResource,
                                             ConsentAccessContext context)
            throws ValidationFailureException, ExtensionException {

        String clientId = context.getClientId();
        if (clientId != null && !StringUtils.equals(clientId, consentResource.getClientId())) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.NOT_FOUND,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.RESOURCE_UNKNOWN_404, ErrorConstants.NO_CONSENT_FOR_CLIENT_ERROR));
        }

        String userId = context.getUserId();
        if (userId == null) {
            return;
        }
        if (consentResource.getAuthorizations() != null) {
            for (StoredAuthorization authorization : consentResource.getAuthorizations()) {
                if (StringUtils.equals(userId, authorization.getUserId())) {
                    return;
                }
            }
        }
        throw new ValidationFailureException(ValidationFailureException.ErrorCode.UNAUTHORIZED,
                ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                        TPPMessage.CodeEnum.PSU_CREDENTIALS_INVALID, ErrorConstants.NO_MATCHING_USER_FOR_CONSENT));
    }

    /**
     * Validates that the service the request path belongs to matches the type of the consent being accessed.
     *
     * @param consentType  type of the consent being accessed
     * @param resourcePath path of the resource being accessed
     * @throws ValidationFailureException if the path does not belong to the consent type
     * @throws ExtensionException if construction of an error failed
     */
    public static void validateResourcePathAgainstConsentType(String consentType, String resourcePath)
            throws ValidationFailureException, ExtensionException {

        String servicePath = getServicePathIdentifier(resourcePath);
        boolean matches;

        if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString(), consentType)) {
            matches = StringUtils.equals(servicePath, CommonConstants.ACCOUNTS_SUBMISSION_PATH_IDENTIFIER)
                    || StringUtils.equals(servicePath, CommonConstants.CARD_ACCOUNTS_SUBMISSION_PATH_IDENTIFIER);
        } else if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString(), consentType)) {
            matches = StringUtils.equals(servicePath,
                    CommonConstants.FUNDS_CONFIRMATIONS_SUBMISSION_PATH_IDENTIFIER);
        } else {
            // A payment consent is addressed on the path of the payment service it was initiated under
            matches = StringUtils.equals(servicePath, consentType);
        }

        if (!matches) {
            throw new ValidationFailureException(ValidationFailureException.ErrorCode.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_INVALID, ErrorConstants.CONSENT_ID_TYPE_MISMATCH));
        }
    }

    /**
     * Checks whether the consent type is one of the payment types.
     *
     * @param consentType type of the consent
     * @return true if the consent is a payment consent
     */
    public static boolean isPaymentConsent(String consentType) {

        return StringUtils.equals(consentType, ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString())
                || StringUtils.equals(consentType, ExtensionEnums.ConsentTypeEnum.BULK_PAYMENTS.toString())
                || StringUtils.equals(consentType, ExtensionEnums.ConsentTypeEnum.PERIODIC_PAYMENTS.toString());
    }
}
