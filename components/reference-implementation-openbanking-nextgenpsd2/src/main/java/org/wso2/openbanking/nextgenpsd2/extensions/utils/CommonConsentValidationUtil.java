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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ExtensionConfiguration;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.AuthorizationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.AccountReference;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.Account;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PopulateConsentAuthorizeScreenData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentCreationRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.PreProcessConsentRetrievalData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredAuthorization;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredBasicConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.StoredDetailedConsentResourceData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseConsentRevocation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponseConsentRevocationData;
import org.wso2.openbanking.nextgenpsd2.extensions.model.consent.SuccessResponsePopulateConsentAuthorizeScreenData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.core.Response;

/**
 * Common utility class for handling consent operations.
 */
public final class CommonConsentValidationUtil {

    private CommonConsentValidationUtil() {
    }

    private static final Log log = LogFactory.getLog(CommonConsentValidationUtil.class);
    private static final Pattern UUID_PATTERN = Pattern.compile
            ("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
                    Pattern.CASE_INSENSITIVE);
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    /**
     * Extracts the account reference number from the provided account reference object.
     *
     * @param accountRefObject account reference object
     * @return account reference number
     */
    public static String getAccountReference(JSONObject accountRefObject) {

        String accountReference = "";

        if (accountRefObject.has(CommonConstants.IBAN)) {
            accountReference = accountRefObject.getString(CommonConstants.IBAN);
        } else if (accountRefObject.has(CommonConstants.BBAN)) {
            accountReference = accountRefObject.getString(CommonConstants.BBAN);
        } else if (accountRefObject.has(CommonConstants.PAN)) {
            accountReference = accountRefObject.getString(CommonConstants.PAN);
        } else if (accountRefObject.has(CommonConstants.MASKED_PAN)) {
            accountReference = accountRefObject.getString(CommonConstants.MASKED_PAN);
        } else if (accountRefObject.has(CommonConstants.MSISDN)) {
            accountReference = accountRefObject.getString(CommonConstants.MSISDN);
        }

        return accountReference;
    }

    /**
     * Get api version from consent type.
     * eg: consents for funds-confirmation was introduced in v2 extension of the nextGenPSD2 specification
     *
     * @param consentType type of consent
     * @return api version
     */
    public static String getApiVersion(String consentType) {
        if (ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString().equals(consentType)) {
            return ExtensionConfiguration.getAisApiVersion();
        }

        if (ExtensionEnums.ConsentTypeEnum.isPaymentType(consentType)) {
            return ExtensionConfiguration.getPisApiVersion();
        }

        if (ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString().equals(consentType)) {
            return ExtensionConfiguration.getPiisApiVersion();
        }

        return "";
    }

    /**
     * Constructs the consent status GET response.
     *
     * @param consentResource the current consent resource
     * @param consentType     the consent type
     * @param payloadToSend   the response payload
     */
    public static void appendConsentStatusResponse(StoredBasicConsentResourceData consentResource,
                                                   String consentType, JSONObject payloadToSend) {

        if (StringUtils.equals(ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString(), consentType)
                || StringUtils.equals(ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString(), consentType)) {
            payloadToSend.put(CommonConstants.CONSENT_STATUS,
                    consentResource.getStatus());
        } else {
            payloadToSend.put(CommonConstants.TRANSACTION_STATUS,
                    consentResource.getStatus());
        }
    }

    /**
     * Checks if the account refs has an unsupported account ref.
     *
     * @param accountRefKeys account reference types sent in the initiation payload
     * @return true if account references has un supported account ref type
     */
    private static boolean hasUnSupportedAccountRefTypes(Set<String> accountRefKeys) {

        List<String> configuredAccountRefTypes = ExtensionConfiguration.getSupportedAccountReferenceTypes();
        for (String accountRef : accountRefKeys) {
            // Skipping currency since it is not an account reference type
            if (StringUtils.equals(accountRef, CommonConstants.CURRENCY)) {
                continue;
            }
            if (!configuredAccountRefTypes.contains(accountRef)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Validates a format of UUID.
     *
     * @param stringUuid string of UUID format
     * @return whether the sting matches UUID pattern
     */
    public static boolean isValidUuid(String stringUuid) {
        return UUID_PATTERN.matcher(stringUuid.trim()).matches();
    }

    /**
     * Method to parse a provided date to ISO date. Throws an error is the provided date is invalid.
     *
     * @param dateToParse date to parse as a string
     * @param errorCode error code for the error to throw if parsing failed
     * @param errorMessage error message for the error to throw is parsing failed
     * @return string parsed to a LocalData object
     * @throws ValidationFailureException if parsing failed
     * @throws ExtensionException if construction of error in nextGenPSD2 format failed
     */
    public static LocalDate parseDateToISO(String dateToParse, TPPMessage.CodeEnum errorCode,
                                           String errorMessage)
            throws ValidationFailureException, ExtensionException {

        LocalDate parsedDate;

        try {
            parsedDate = LocalDate.parse(dateToParse, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR, errorCode, errorMessage), e);
        }
        return parsedDate;
    }

    /**
     * Validates the consent client ID with the registered client ID.
     *
     * @param registeredClientId the registered client id
     * @param consentClientId    the client id of the current consent
     */
    public static void validateClient(String registeredClientId, String consentClientId)
            throws ValidationFailureException, ExtensionException {

        if (!StringUtils.equals(registeredClientId, consentClientId)) {
            throw new ValidationFailureException(Response.Status.FORBIDDEN,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.RESOURCE_UNKNOWN, ErrorConstants.NO_CONSENT_FOR_CLIENT_ERROR));
        }
    }

    /**
     * Validates the request consent type with the type of the current consent.
     *
     * @param requestConsentType     the consent type which the request belongs to
     * @param typeOfRetrievedConsent the consent type of the current consent
     */
    public static void validateConsentType(String requestConsentType, String typeOfRetrievedConsent)
            throws ValidationFailureException, ExtensionException {

        if (!StringUtils.equals(requestConsentType, typeOfRetrievedConsent)) {
            throw new ValidationFailureException(Response.Status.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_INVALID, ErrorConstants.CONSENT_ID_TYPE_MISMATCH));
        }
    }

    /**
     * Builds consent revocation response.
     *
     * @param requestBody request body from the pre-process consent revocation request
     * @return success response for consent revocation
     */
    public static SuccessResponseConsentRevocation buildConsentRevocationResponse(
            PreProcessConsentRequestBody requestBody) {
        SuccessResponseConsentRevocation validationResponse = new SuccessResponseConsentRevocation();
        validationResponse.setResponseId(requestBody.getRequestId());
        validationResponse.setStatus(SuccessResponseConsentRevocation.StatusEnum.SUCCESS);

        // Set revocation response data
        StoredBasicConsentResourceData consentData = requestBody.getData().getConsentResource();
        SuccessResponseConsentRevocationData responseData = new SuccessResponseConsentRevocationData();
        if (ExtensionEnums.ConsentTypeEnum.isPaymentType(consentData.getType())) {
            responseData.setRevocationStatusName(ExtensionEnums.TransactionStatusEnum.CANC.name());
        } else {
            responseData.setRevocationStatusName(ExtensionEnums.ConsentStatusEnum.TERMINATED_BY_TPP.toString());
        }
        responseData.setRequireTokenRevocation(String.valueOf(isTokenRevocationRequired(consentData)));

        validationResponse.setData(responseData);
        return validationResponse;
    }

    /**
     * Decide if token revocation is necessary given the status of consent. A token can only exist for a
     * consent that was authorized, or for a transaction that was accepted.
     *
     * @param consentData consent data from request
     * @return whether token revocation is required
     */
    private static boolean isTokenRevocationRequired(StoredBasicConsentResourceData consentData) {
        //ToDo: Verify that these are the only statuses of transaction where a token revocation would be necessary
        return ExtensionEnums.ConsentStatusEnum.VALID.toString().equals(consentData.getStatus())
                || ExtensionEnums.TransactionStatusEnum.ACCP.name().equals(consentData.getStatus());
    }

    /**
     * Validates revoke request for payment, account and funds confirmation consents and returns built response.
     *
     * @param requestBody consent revocation request body
     * @return success response for consent revocation
     * @throws ExtensionException if the request body is malformed
     * @throws ValidationFailureException if consent revocation request fails validations
     */
    public static SuccessResponseConsentRevocation
    validateRevokeRequestAndReturnResponse(PreProcessConsentRequestBody requestBody) throws ExtensionException,
            ValidationFailureException {
        String requestId = requestBody.getRequestId();

        PreProcessConsentRetrievalData data = requestBody.getData();
        StoredBasicConsentResourceData consentResource = data.getConsentResource();
        String requestPath = data.getConsentResourcePath();
        String consentType = ConsentPathUtil.getConsentTypeFromRequestPath(requestPath);
        String consentId = requestBody.getData().getConsentId();

        // Validate client
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating consent of Id %s for valid client", requestId, consentId));
        }

        // Get request client id from the headers
        JSONObject headers = JsonUtil.convertObjectToJson(data.getRequestHeaders());
        validateClient(HeaderUtil.getClientId(headers), data.getConsentResource().getClientId());

        // Validate consent type
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating consent of Id %s for correct type", requestId, consentId));
        }
        CommonConsentValidationUtil.validateConsentType(consentType, consentResource.getType());

        // Validate consent is revocable (single payments cannot be revoked)
        CommonConsentValidationUtil.validateIfConsentTypeIsRevocable(consentType);

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Verify if the consent is already revoked", requestId));
        }
        if (StringUtils.equals(ExtensionEnums.ConsentStatusEnum.REVOKED_BY_PSU.toString(), consentResource.getStatus())
                || StringUtils.equals(ExtensionEnums.ConsentStatusEnum.TERMINATED_BY_TPP.toString(),
                consentResource.getStatus())) {
            throw new ValidationFailureException(Response.Status.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_INVALID, ErrorConstants.CONSENT_ALREADY_DELETED));
        }

        // Check whether the consent is already expired before deleting
        if (StringUtils.equals(ExtensionEnums.ConsentStatusEnum.EXPIRED.toString(), consentResource.getStatus())) {
            throw new ValidationFailureException(Response.Status.UNAUTHORIZED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_INVALID, ErrorConstants.CONSENT_ALREADY_EXPIRED));
        }

        return buildConsentRevocationResponse(requestBody);
    }

    /**
     * Validates if the consent type is revocable.
     *
     * @param consentType consent type of consent to revoke
     * @throws ValidationFailureException if consent is irrevocable
     */
    private static void validateIfConsentTypeIsRevocable(String consentType)
            throws ValidationFailureException, ExtensionException {
        if (ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString().equals(consentType)) {
            throw new ValidationFailureException(Response.Status.METHOD_NOT_ALLOWED,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CANCELLATION_INVALID,
                            ErrorConstants.CANCELLATION_NOT_APPLICABLE));
        }
    }

    /**
     * Disables default violation and sets built constraint violation.
     *
     * @param context context of the constraint validator
     * @param violationMessage violation message to return
     */
    public static void setConstraintViolation(ConstraintValidatorContext context, String violationMessage) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(violationMessage).addConstraintViolation();
    }

    /**
     * Builds constraint violation message with error and error code.
     * Sets FORMAT_ERROR as default.
     * overload method for {@link #buildViolationMessage(TPPMessage.CodeEnum, String)} with default error code.
     *
     * @param error error message to return from violation
     * @return built violation message
     */
    public static String buildViolationMessage(String error) {
        return buildViolationMessage(TPPMessage.CodeEnum.FORMAT_ERROR, error);
    }

    /**
     * Builds constraint violation message with error and error code.
     *
     * @param errorCode error code to return from violation
     * @param error error message to return from violation
     * @return built violation message
     */
    public static String buildViolationMessage(TPPMessage.CodeEnum errorCode, String error) {
        return errorCode.toString() + ":" + error;
    }

    /**
     * Splits retrieved violation message to error and error code.
     *
     * @param violationMessage breaks violation message to get error code and error message
     * @return split error code and error message
     */
    public static String[] splitViolationMessage(String violationMessage) {
        return violationMessage.split(":", 2);
    }

    /**
     * Validates the given JSON payload against a model class using Hibernate Validator.
     * This method expects all violation messages to be built by
     * {@link CommonConsentValidationUtil#buildViolationMessage(TPPMessage.CodeEnum, String)}
     *
     * @param jsonPayload   JSON string representing the model payload
     * @param modelClass    Class object of the model
     * @param <T>   The model type to be validated
     * @return  validated and mapped object
     * @throws ValidationFailureException   if deserialization or validation fails
     */
    public static <T> T validateJSONFromModel(String jsonPayload, Class<T> modelClass)
            throws ValidationFailureException, ExtensionException {
        T mappedObject;

        // Map to object
        try {
            mappedObject = JsonUtil.readValue(jsonPayload, modelClass);
        } catch (JsonProcessingException e) {
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.PAYLOAD_FORMAT_ERROR), e);
        }

        // Find violations
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(mappedObject);

        // Throw first validation error from validation failures
        if (!violations.isEmpty()) {
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    buildViolationError(violations.iterator().next().getMessage()));
        }

        return mappedObject;
    }

    /**
     * Builds the error reported for a constraint violation. The error code is read from the violation message,
     * which is built by {@link #buildViolationMessage(TPPMessage.CodeEnum, String)}. A message carrying no known
     * error code, as a constraint of a stock annotation reports, is returned as it stands under a format error,
     * so that an unannotated constraint reports the violation rather than failing the request.
     *
     * @param violationMessage violation message reported by the validator
     * @return the violation as a nextGenPSD2 error
     * @throws ExtensionException if construction of the error failed
     */
    private static JSONObject buildViolationError(String violationMessage) throws ExtensionException {
        String[] codeAndMessage = splitViolationMessage(violationMessage);

        if (codeAndMessage.length == 2) {
            try {
                return ErrorUtil.constructBerlinError("payload", TPPMessage.CategoryEnum.ERROR,
                        TPPMessage.CodeEnum.valueOf(codeAndMessage[0]), codeAndMessage[1]);
            } catch (IllegalArgumentException e) {
                log.warn(String.format("Validation constraint reported the unknown error code %s. Reporting the "
                        + "violation as a format error.", codeAndMessage[0].replaceAll("[\r\n]", "")));
            }
        }

        return ErrorUtil.constructBerlinError("payload", TPPMessage.CategoryEnum.ERROR,
                TPPMessage.CodeEnum.FORMAT_ERROR, violationMessage);
    }

    /**
     * Returns the extracted account reference type from the account reference object.
     *
     * @param accountRefObject account reference JSON object
     * @return account reference type
     */
    public static String getAccountReferenceType(JSONObject accountRefObject) {

        List<String> configuredAccountReferences = ExtensionConfiguration.getSupportedAccountReferenceTypes();
        for (String accountRef : configuredAccountReferences) {
            if (accountRefObject.has(accountRef)) {
                return accountRef;
            }
        }
        return null;
    }

    /**
     * Returns the extracted account reference type from the account reference object.
     *
     * @param accountRefObject account reference map object
     * @return account reference type
     */
    public static String getAccountReferenceType(Map<String, ?> accountRefObject) {

        List<String> configuredAccountReferences = ExtensionConfiguration.getSupportedAccountReferenceTypes();
        for (String accountRef : configuredAccountReferences) {
            if (accountRefObject.containsKey(accountRef)) {
                return accountRef;
            }
        }
        return null;
    }

    /**
     * Populates consent initiated accounts for both payment and funds confirmation consents.
     *
     * @param responseData response to the request made to populate-consent-authorize-screen
     * @param requestData request made to populate-consent-authorize-screen
     * @param accountRefJSON account to include under initiated account for consent
     * @throws AuthorizationFailureException if no accounts were found for the user
     */
    public static void populateConsentInitiatedAccounts(SuccessResponsePopulateConsentAuthorizeScreenData responseData,
                                                        PopulateConsentAuthorizeScreenData requestData,
                                                        JSONObject accountRefJSON)
            throws AuthorizationFailureException {
        String payableAccountsEndpoint = ExtensionConfiguration.getPayableAccountsEndpoint();
        JSONArray userAccountsArray = DataRetrievalUtil.getUserAccounts(requestData.getUserId(),
                payableAccountsEndpoint, new HashMap<>(), new HashMap<>());

        if (userAccountsArray == null || userAccountsArray.isEmpty()) {
            throw new AuthorizationFailureException(ErrorConstants.ACCOUNTS_NOT_FOUND_FOR_USER);
        }

        Account validatedAccountRefObject;
        if (accountRefJSON.has(CommonConstants.MASKED_PAN)) {
            // Skipping validation for maskedPan based account reference types and this needs to be validated
            // from the bank back end since there might be scenarios where there are 2 similar maskedPans
            // for a single user therefore we are not sure which account to validate it against
            // Eg: 123456xxxxxx1234, 123456xxxxxx1234 -> Both these maskedPans can belong to the same user
            validatedAccountRefObject = ConsentAuthorizationUtil.getAccountFromAccountRef(accountRefJSON);
        } else {
            JSONArray accountRefsArray = new JSONArray();
            accountRefsArray.put(accountRefJSON);
            List<Account> validatedAccountList = ConsentAuthorizationUtil.getValidatedAccountObjects(accountRefsArray,
                    userAccountsArray);

            // Validate account existence under user
            if (validatedAccountList == null || validatedAccountList.isEmpty()) {
                throw new AuthorizationFailureException(ErrorConstants.ACCOUNTS_NOT_FOUND_FOR_USER);
            }

            validatedAccountRefObject = validatedAccountList.get(0);
        }

        if (validatedAccountRefObject == null) {
            throw new AuthorizationFailureException(ErrorConstants.ACCOUNTS_NOT_FOUND_FOR_USER);
        }

        // Add to consent page to be displayed
        responseData.getConsentData().setInitiatedAccountsForConsent(List.of(validatedAccountRefObject));
    }

    /**
     * Extracts account reference object from authorized account objects.
     *
     * @param accounts account objects under user granted data
     * @return list of account reference objects
     */
    public static List<AccountReference> extractAccountRef(List<Account> accounts) {
        List<AccountReference> accountRefs = new ArrayList<>();

        for (Account authorizedAccount: accounts) {
            AccountReference accountRef = new AccountReference();
            if (authorizedAccount.getAdditionalProperties().containsKey(CommonConstants.CURRENCY)) {
                accountRef.setAdditionalProperties(CommonConstants.CURRENCY,
                        (String) authorizedAccount.getAdditionalProperties().get(CommonConstants.CURRENCY));
            }
            String accountRefType = CommonConsentValidationUtil
                    .getAccountReferenceType(authorizedAccount.getAdditionalProperties());
            accountRef.setAdditionalProperties(accountRefType,
                    (String) authorizedAccount.getAdditionalProperties().get(accountRefType));

            accountRefs.add(accountRef);
        }

        return accountRefs;
    }

    /**
     * Validates the existence and format of consent initiation payload.
     *
     * @param requestBody pre process consent creation request body
     * @throws ExtensionException         if object to JSON conversion fails
     * @throws ValidationFailureException if payload format validation fails
     */
    public static void validatePayloadFormat(PreProcessConsentCreationRequestBody requestBody)
            throws ExtensionException, ValidationFailureException {
        JSONObject consentInitiationDataJSON;
        try {
            consentInitiationDataJSON = JsonUtil.convertObjectToJson(requestBody.getData().getConsentInitiationData());

            if (consentInitiationDataJSON.isEmpty()) {
                // If payload is empty
                throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                        ErrorUtil.constructBerlinError("payload", TPPMessage.CategoryEnum.ERROR,
                                TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.PAYLOAD_NOT_PRESENT_ERROR));
            }

        } catch (JSONException e) {
            // If payload is not JSON
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("payload", TPPMessage.CategoryEnum.ERROR,
                    TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.PAYLOAD_FORMAT_ERROR), e);
        }
    }

    /**
     * Validates that the client requesting consent authorization is the same client that initiated the consent.
     *
     * @param consentResource stored consent resource from the accelerator
     * @param queryParams query parameters sent with the authorization request
     * @throws AuthorizationFailureException if client id validation failed
     */
    public static void validateAuthorizingClient(StoredDetailedConsentResourceData consentResource,
                                                 JSONObject queryParams)
            throws ExtensionException, AuthorizationFailureException {
        try {
            String clientId = queryParams.getString(CommonConstants.CLIENT_ID_PARAM);
            validateClient(clientId, consentResource.getClientId());
        } catch (JSONException e) {
            throw new AuthorizationFailureException(ErrorConstants.CLIENT_ID_NOT_FOUND, e);
        } catch (ValidationFailureException e) {
            throw new AuthorizationFailureException(ErrorConstants.NO_CONSENT_FOR_CLIENT_ERROR, e);
        }
    }

    /**
     * Verifies that the request scope matches the authorizing consent type.
     *
     * @param requestId ID of the request for logging
     * @param queryParams query parameters from the authorization request
     * @param consentType type of the consent
     * @throws AuthorizationFailureException if scope validation failed
     */
    public static void validateScope(String requestId, JSONObject queryParams, String consentType)
            throws AuthorizationFailureException {
        String scopes = queryParams.optString(CommonConstants.SCOPE_PARAM);
        if (!scopes.isEmpty()) {
            ConsentAuthorizationUtil.validateConsentTypeWithScopes(requestId, consentType, scopes);
        } else {
            throw new AuthorizationFailureException("Scope not found in request");
        }
    }

    /**
     * Appends the authorization resource being authorized to consent metadata.
     *
     * @param responseData success response data for populating consent authorization screen
     * @param unauthorizedObj authorization resource being authorized
     */
    public static void appendAuthorizationToResponse(SuccessResponsePopulateConsentAuthorizeScreenData responseData,
                                                     StoredAuthorization unauthorizedObj) {
        Map<String, Object> consentMetadata;
        if (responseData.getConsentData().getConsentMetadata() == null) {
            consentMetadata = new HashMap<>();
        } else {
            consentMetadata = (Map<String, Object>) responseData.getConsentData().getConsentMetadata();
        }
        consentMetadata.put(CommonConstants.AUTHORIZING_AUTHORIZATION, unauthorizedObj);
        responseData.getConsentData().setConsentMetadata(consentMetadata);
    }

    /**
     * Extracts authorization resource being authorized from consent metadata.
     *
     * @param retrievalMetadata metadata stored at populate consent authorize screen endpoint
     * @return authorization resource to authorize mapped to an object
     * @throws ExtensionException if the authorization resource stored in metadata is invalid
     */
    public static StoredAuthorization extractAuthorizingResource(JSONObject retrievalMetadata)
            throws ExtensionException {
        StoredAuthorization authorizingResource;
        try {
            authorizingResource = JsonUtil.readValue(retrievalMetadata
                    .getJSONObject(CommonConstants.AUTHORIZING_AUTHORIZATION).toString(),
                    StoredAuthorization.class);
        } catch (JsonProcessingException e) {
            // Should be unreachable given that a validated authorization resource is attached
            // to metadata when populating consent page
            throw new ExtensionException(Response.Status.BAD_REQUEST, ErrorConstants.INVALID_REQUEST,
                    ErrorConstants.AUTHORIZING_AUTHORIZATION_INVALID, e);
        }
        return authorizingResource;
    }

}
