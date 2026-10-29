/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.openbanking.berlin.demo.backend.utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.ACCESS;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.ACCOUNTS;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.ACCOUNT_NOT_IN_CONSENT;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.ACCOUNT_ID;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.ACCOUNT_ID_SEPARATOR;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.ACCOUNT_KEY_SEPARATOR;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.ACTIVE;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.ANY_CURRENCY;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.CODE;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.CONSENT_MAPPING_RESOURCES;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.CURRENCY;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.IBAN;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.MAPPING_STATUS;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.MESSAGE;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.PERMISSION;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.RECEIPT;
import static org.wso2.openbanking.berlin.demo.backend.utils.Constants.AccountService.VALID;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * Utility methods.
 */
public class Utility {

    private static final Log log = LogFactory.getLog(Utility.class);

    private Utility() {

    }

    /**
     * Check if request ID and consent ID are set.
     *
     * @param requestID ID of the request.
     * @param consentID ID gotten after consent transaction.
     * @return JSON response with either an error message or "valid".
     */
    public static JSONObject validateRequestHeader(String requestID, String consentID) {

        JSONObject responseJSON = new JSONObject();
        if (StringUtils.isBlank(requestID)) {
            responseJSON.put(CODE, FORBIDDEN.getStatusCode());
            responseJSON.put(MESSAGE, "X-Request_ID not found");
            return responseJSON;
        } else if (StringUtils.isBlank(consentID)) {
            responseJSON.put(CODE, FORBIDDEN.getStatusCode());
            responseJSON.put(MESSAGE, "Consent-ID not found");
            return responseJSON;
        } else {
            responseJSON.put(MESSAGE, VALID);
            return responseJSON;
        }
    }

    /**
     * Check if request ID is set.
     *
     * @param requestID ID of the request.
     * @return JSON response with either an error message or "valid".
     */
    public static JSONObject validatePaymentRequestHeader(String requestID) {

        JSONObject responseJSON = new JSONObject();
        if (StringUtils.isBlank(requestID)) {
            responseJSON.put(CODE, FORBIDDEN.getStatusCode());
            responseJSON.put(MESSAGE, "X-Request_ID not found");
        } else {
            responseJSON.put(MESSAGE, VALID);
        }
        return responseJSON;
    }

    /**
     * Extracts the accounts that the consent grants access to from the Account-Request-Information header.
     *
     * <p>The header carries the consent as a JWT. The accounts are taken from the consent mapping resources that
     * hold the "accounts" permission, which is the set the PSU authorised. When the consent carries no mapping
     * resources, the accounts requested in the consent receipt are used instead.</p>
     *
     * @param accountRequestInformation value of the Account-Request-Information header
     * @return keys of the permitted accounts, or null if the permitted accounts could not be determined
     */
    public static Set<String> getPermittedAccountKeys(String accountRequestInformation) {

        JSONObject consent = decodeConsentFromHeader(accountRequestInformation);
        if (consent == null) {
            return null;
        }

        Set<String> permittedAccountKeys = readKeysFromMappingResources(consent);
        if (permittedAccountKeys == null) {
            permittedAccountKeys = readKeysFromReceipt(consent);
        }

        // The consent was read, so it decides the accounts even when it grants access to none of them
        return permittedAccountKeys == null ? Collections.emptySet() : permittedAccountKeys;
    }

    /**
     * Checks that the account addressed by the request is among the accounts the consent grants access to.
     *
     * <p>The account is addressed by its reference alone, so a consent on any currency of that account grants
     * access to it. The account is not rejected when the Account-Request-Information header is absent or cannot be
     * read, so that the endpoints keep working for callers that do not go through the accelerator.</p>
     *
     * @param accountID                 account reference taken from the request path
     * @param accountRequestInformation value of the Account-Request-Information header
     * @return JSON response with either an error message or "valid".
     */
    public static JSONObject validateAccountAccess(String accountID, String accountRequestInformation) {

        JSONObject responseJSON = new JSONObject();

        Set<String> permittedAccountKeys = getPermittedAccountKeys(accountRequestInformation);
        if (permittedAccountKeys == null) {
            log.debug("Accounts of the consent could not be determined. Account access is not restricted.");
            responseJSON.put(MESSAGE, VALID);
            return responseJSON;
        }

        if (isAccountReferencePermitted(permittedAccountKeys, accountID)) {
            responseJSON.put(MESSAGE, VALID);
        } else {
            log.info("The account addressed by the request is not granted access to by the consent.");
            responseJSON.put(CODE, FORBIDDEN.getStatusCode());
            responseJSON.put(MESSAGE, ACCOUNT_NOT_IN_CONSENT);
        }
        return responseJSON;
    }

    /**
     * Checks whether any currency of an account of the bank is among the accounts the consent grants access to.
     *
     * @param permittedAccountKeys keys of the permitted accounts
     * @param iban                 IBAN of the account of the bank
     * @return true if the consent grants access to the account in any currency, false otherwise
     */
    public static boolean isAccountReferencePermitted(Set<String> permittedAccountKeys, String iban) {

        if (StringUtils.isBlank(iban)) {
            return false;
        }

        String accountPrefix = iban.trim().toUpperCase() + ACCOUNT_KEY_SEPARATOR;
        return permittedAccountKeys.stream().anyMatch(key -> key.startsWith(accountPrefix));
    }

    /**
     * Checks whether an account of the bank is among the accounts the consent grants access to.
     *
     * <p>An account reference permitted without a currency matches every currency of that account, which is how a
     * consent on a multicurrency account addresses all of its sub accounts.</p>
     *
     * @param permittedAccountKeys keys of the permitted accounts
     * @param iban                 IBAN of the account of the bank
     * @param currency             currency of the account of the bank
     * @return true if the account is permitted, false otherwise
     */
    public static boolean isAccountPermitted(Set<String> permittedAccountKeys, String iban, String currency) {

        if (StringUtils.isBlank(iban)) {
            return false;
        }
        return permittedAccountKeys.contains(buildAccountKey(iban, currency))
                || permittedAccountKeys.contains(buildAccountKey(iban, ANY_CURRENCY));
    }

    /**
     * Decodes the consent carried as a JWT by the Account-Request-Information header.
     *
     * <p>The header is produced by the accelerator for the backend, so only its payload is read here. The signature
     * is not verified by this mock backend.</p>
     *
     * @param accountRequestInformation value of the Account-Request-Information header
     * @return the consent, or null if the header is absent or cannot be read
     */
    private static JSONObject decodeConsentFromHeader(String accountRequestInformation) {

        if (StringUtils.isBlank(accountRequestInformation)) {
            log.debug("Account-Request-Information header is not present.");
            return null;
        }

        String[] jwtParts = accountRequestInformation.trim().split("\\.");
        if (jwtParts.length < 2) {
            log.warn("Account-Request-Information header is not a JWT. Accounts cannot be filtered.");
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8);
            Object parsedPayload = JSONValue.parse(payload);
            if (parsedPayload instanceof JSONObject) {
                return (JSONObject) parsedPayload;
            }
            log.warn("Payload of the Account-Request-Information header is not a JSON object. Accounts cannot be " +
                    "filtered.");
        } catch (IllegalArgumentException e) {
            log.warn("Payload of the Account-Request-Information header could not be decoded. Accounts cannot be " +
                    "filtered.", e);
        }
        return null;
    }

    /**
     * Reads the permitted account keys from the active consent mapping resources holding the accounts permission.
     *
     * @param consent consent decoded from the Account-Request-Information header
     * @return the permitted account keys, or null if the consent carries no mapping resources
     */
    private static Set<String> readKeysFromMappingResources(JSONObject consent) {

        Object mappingResources = consent.get(CONSENT_MAPPING_RESOURCES);
        if (!(mappingResources instanceof JSONArray) || ((JSONArray) mappingResources).isEmpty()) {
            return null;
        }

        Set<String> permittedAccountKeys = new HashSet<>();
        for (Object mappingResource : (JSONArray) mappingResources) {
            if (!(mappingResource instanceof JSONObject)) {
                continue;
            }
            JSONObject mapping = (JSONObject) mappingResource;
            if (!ACCOUNTS.equalsIgnoreCase(asString(mapping.get(PERMISSION)))
                    || !ACTIVE.equalsIgnoreCase(asString(mapping.get(MAPPING_STATUS)))) {
                continue;
            }

            String accountKey = toAccountKey(asString(mapping.get(ACCOUNT_ID)));
            if (accountKey != null) {
                permittedAccountKeys.add(accountKey);
            }
        }
        return permittedAccountKeys;
    }

    /**
     * Reads the permitted account keys from the accounts requested in the consent receipt.
     *
     * @param consent consent decoded from the Account-Request-Information header
     * @return the permitted account keys, or null if the receipt carries no accounts
     */
    private static Set<String> readKeysFromReceipt(JSONObject consent) {

        Object receipt = consent.get(RECEIPT);
        if (!(receipt instanceof JSONObject)) {
            return null;
        }
        Object access = ((JSONObject) receipt).get(ACCESS);
        if (!(access instanceof JSONObject)) {
            return null;
        }
        Object accounts = ((JSONObject) access).get(ACCOUNTS);
        if (!(accounts instanceof JSONArray)) {
            return null;
        }

        Set<String> permittedAccountKeys = new HashSet<>();
        for (Object account : (JSONArray) accounts) {
            if (!(account instanceof JSONObject)) {
                continue;
            }
            JSONObject accountReference = (JSONObject) account;
            String iban = asString(accountReference.get(IBAN));
            if (StringUtils.isNotBlank(iban)) {
                permittedAccountKeys.add(buildAccountKey(iban, asString(accountReference.get(CURRENCY))));
            }
        }
        return permittedAccountKeys;
    }

    /**
     * Converts an account identifier of a consent mapping resource into an account key.
     *
     * <p>The identifier is built as "&lt;reference type&gt;:&lt;reference&gt;" and carries the currency as a third
     * segment when the consent addresses a single currency of a multicurrency account.</p>
     *
     * @param accountId account identifier of a consent mapping resource, e.g. "iban:DE12345678901234567890:USD"
     * @return the account key, or null if the identifier carries no account reference
     */
    private static String toAccountKey(String accountId) {

        if (StringUtils.isBlank(accountId)) {
            return null;
        }

        String[] segments = accountId.split(ACCOUNT_ID_SEPARATOR);
        if (segments.length == 1) {
            // The identifier carries the account reference on its own
            return buildAccountKey(segments[0], null);
        }
        if (segments.length == 2) {
            return buildAccountKey(segments[1], null);
        }
        return buildAccountKey(segments[1], segments[2]);
    }

    /**
     * Builds the key an account is matched by.
     *
     * @param iban     IBAN of the account
     * @param currency currency of the account, matching every currency when blank
     * @return the account key
     */
    private static String buildAccountKey(String iban, String currency) {

        String currencyKey = StringUtils.isBlank(currency) ? ANY_CURRENCY : currency.trim().toUpperCase();
        return iban.trim().toUpperCase() + ACCOUNT_KEY_SEPARATOR + currencyKey;
    }

    /**
     * Returns the given value as a string.
     *
     * @param value value read from the consent
     * @return the value as a string, or null if it is not set
     */
    private static String asString(Object value) {

        return value == null ? null : String.valueOf(value);
    }

}
