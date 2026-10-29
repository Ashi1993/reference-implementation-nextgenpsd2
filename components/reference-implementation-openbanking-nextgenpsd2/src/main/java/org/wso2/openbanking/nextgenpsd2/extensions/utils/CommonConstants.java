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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Common constants.
 */
public class CommonConstants {

    public static final String SCA_TYPE = "Type";
    public static final String SCA_VERSION = "Version";
    public static final String SCA_ID = "Id";
    public static final String SCA_NAME = "Name";
    public static final String SCA_MAPPED_APPROACH = "MappedApproach";
    public static final String SCA_DESCRIPTION = "Description";
    public static final String SCA_DEFAULT = "Default";
    public static final String X_WSO2_CLIENT_ID_KEY = "x-wso2-client-id";
    public static final String CLIENT_ID_PARAM = "client_id";
    public static final String SCOPE_PARAM = "scope";
    public static final String SCA_APPROACH_KEY = "SCA-Approach";
    public static final String SCA_METHODS_KEY = "SCA-Methods";
    public static final String AIS_SCOPE = "ais";
    public static final String PIS_SCOPE = "pis";
    public static final String PIIS_SCOPE = "piis";
    public static final String CONSENT_ID = "consentId";
    public static final String PAYMENT_ID = "paymentId";
    public static final String DELIMITER = ":";
    public static final String CURRENCY = "currency";
    public static final String IS_DEFAULT = "isDefault";
    public static final String EXPLICIT_AUTHORISATION_PATH_END = "authorisations";
    public static final String PSU_IP_ADDRESS_PROPER_CASE_HEADER = "PSU-IP-Address";
    public static final String PSU_IP_ADDRESS_HEADER = "psu-ip-address";
    public static final String PSU_ID_HEADER = "psu-id";
    public static final String TPP_REDIRECT_PREFERRED_HEADER = "tpp-redirect-preferred";
    public static final String LAST_ACTION_DATE = "lastActionDate";
    public static final String CONSENT_STATUS = "consentStatus";
    public static final String INSTRUCTED_AMOUNT = "instructedAmount";
    public static final String CHOSEN_SCA_METHOD = "chosenScaMethod";
    public static final String SCA_METHODS = "scaMethods";
    public static final String LINKS = "_links";
    public static final String HREF = "href";
    public static final String SELF_LINK_TEMPLATE = "/%s/%s/%s";
    public static final String STATUS_LINK_TEMPLATE = "/%s/%s/%s/status";
    public static final String AUTH_RESOURCE_LINK_TEMPLATE = "/%s/%s/%s/authorisations/%s";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String UTC = "UTC";
    public static final String INSTRUCTED_AMOUNT_TITLE = "Instructed Amount";
    public static final String INSTRUCTED_CURRENCY_TITLE = "Instructed Currency";
    public static final String CREDITOR_ACCOUNT_CURRENCY_TITLE = "Creditor Account Currency";
    public static final String CREDITOR_NAME_TITLE = "Creditor Name";
    public static final String CREDITOR_REFERENCE_TITLE = "Creditor Account %s Reference";
    public static final String REMITTANCE_INFORMATION_UNSTRUCTURED_TITLE = "Remittance Information Unstructured";
    public static final String END_TO_END_IDENTIFICATION_TITLE = "End to End Identification";
    public static final String START_DATE_TITLE = "Start Date";
    public static final String END_DATE_TITLE = "End Date";
    public static final String FREQUENCY_TITLE = "Frequency";
    public static final String EXECUTION_RULE_TITLE = "Execution Rule";
    public static final String REQUESTED_DATA_TITLE = "Requested Data: ";
    public static final String CONSENT_DETAILS_TITLE = "Consent Details: ";
    public static final String CREDITOR_AGENT_TITLE = "Creditor Agent: ";
    public static final String PAYMENT_TITLE = "Payment ";
    public static final String DEFAULT_PERMISSION = "n/a";
    public static final String ACTIVE = "active";

    // Keys of the data request context the accelerator sends to the validate-consent-access endpoint
    public static final String HEADERS_KEY = "headers";
    public static final String RESOURCE_PARAMS_KEY = "resourceParams";
    public static final String RESOURCE_KEY = "resource";
    public static final String HTTP_METHOD_KEY = "httpMethod";
    public static final String CONTEXT_KEY = "context";
    public static final String CLIENT_ID_KEY = "clientId";
    public static final String USER_ID_KEY = "userId";
    public static final String ELECTED_RESOURCE_KEY = "electedResource";

    public static final String CONSENT_ID_HEADER = "consent-id";
    public static final String CONSENT_ID_PROPER_CASE_HEADER = "Consent-ID";
    public static final String SUPER_TENANT_DOMAIN = "@carbon.super";
    public static final String CARD_ACCOUNTS = "card-accounts";
    public static final String WITH_BALANCE = "withBalance";

    // First path element of a data request, used to recognise the service the request belongs to
    public static final String ACCOUNTS_SUBMISSION_PATH_IDENTIFIER = "accounts";
    public static final String CARD_ACCOUNTS_SUBMISSION_PATH_IDENTIFIER = "card-accounts";
    public static final String FUNDS_CONFIRMATIONS_SUBMISSION_PATH_IDENTIFIER = "funds-confirmations";
    public static final String PAYMENTS_RETRIEVAL_PATH_IDENTIFIER = "payments";
    public static final String BULK_PAYMENTS_RETRIEVAL_PATH_IDENTIFIER = "bulk-payments";
    public static final String PERIODIC_PAYMENTS_RETRIEVAL_PATH_IDENTIFIER = "periodic-payments";

    public static final List<String> BULK_ACCOUNT_ACCESS_METHODS_REGEX_LIST = Collections
            .unmodifiableList(Arrays.asList(
            "/accounts",
            "/accounts\\?withBalance",
            "/card-accounts"));
    public static final List<String> SINGLE_ACCOUNT_ACCESS_METHODS_REGEX_LIST = Collections
            .unmodifiableList(Arrays.asList(
            "/accounts/[^/?]*",
            "/accounts/[^/?]*\\?withBalance",
            "/accounts/[^/?]*/balances",
            "/accounts/[^/?]*/transactions",
            "/accounts/[^/?]*/transactions\\?withBalance",
            "/accounts/[^/?]*/transactions/[^/?]*",
            "/card-accounts/[^/?]*",
            "/card-accounts/[^/?]*/balances",
            "/card-accounts/[^/?]*/transactions"));
    // Original proper case header constants
    public static final String LOCATION_HEADER = "Location";
    public static final String X_REQUEST_ID_PROPER_CASE_HEADER = "X-Request-ID";
    public static final String X_REQUEST_ID_HEADER = "x-request-id";
    // Constants that are used by funds confirmation service
    public static final String CARD_EXPIRY_DATE = "cardExpiryDate";
    public static final String CARD_NUMBER = "cardNumber";
    public static final String CARD_INFORMATION = "cardInformation";
    public static final String CARD_NUMBER_TITLE = "Card Number";
    public static final String  CARD_EXPIRY_DATE_TITLE = "Card Expiry Date";
    public static final String  CARD_INFORMATION_TITLE = "Card Information";
    // Constants that are used by accounts service
    public static final String AUTHORIZING_AUTHORIZATION = "authorizingAuthorization";
    public static final String ACCOUNTS_CONSENT_PATH = "consents";
    public static final String MAXIMUM_VALID_DATE = "9999-12-31";
    public static final String RECURRING_INDICATOR = "recurringIndicator";
    public static final String RECURRING_INDICATOR_TITLE = "Recurring Indicator";
    public static final String VALID_UNTIL = "validUntil";
    public static final String VALID_UNTIL_TITLE = "Consent Expiry Date";
    public static final String FREQUENCY_PER_DAY = "frequencyPerDay";
    public static final String FREQUENCY_PER_DAY_TITLE = "Frequency Per Day";
    public static final String COMBINED_SERVICE_INDICATOR = "combinedServiceIndicator";
    public static final String COMBINED_SERVICE_INDICATOR_TITLE = "Combined Service Indicator";
    public static final String ALL_ACCOUNTS = "allAccounts";
    public static final String ALL_ACCOUNTS_WITH_OWNER_NAME = "allAccountsWithOwnerName";
    public static final String ACCOUNT = "account";
    public static final String ACCOUNTS_LINK_TEMPLATE = "/%s/accounts";
    // Constants that are used in accounts authorize flow
    public static final String ACCOUNTS_PERMISSION = "Read Account Information";
    public static final String BALANCES_PERMISSION = "Read Account Balance Information";
    public static final String TRANSACTIONS_PERMISSION = "Read Account Transaction Information";
    // Constants that are used by payments service
    public static final String PAYMENTS_SERVICE_PATH = "payments";
    public static final String BULK_PAYMENTS_SERVICE_PATH = "bulk-payments";
    public static final String PERIODIC_PAYMENTS_SERVICE_PATH = "periodic-payments";
    public static final String PAYMENT_EXPLICIT_CANCELLATION_AUTHORISATION_PATH_END = "cancellation-authorisations";
    public static final String FOLLOWING_EXECUTION_RULE = "following";
    public static final String PRECEDING_EXECUTION_RULE = "preceding";
    public static final String IBAN = "iban";
    public static final String BBAN = "bban";
    public static final String PAN = "pan";
    public static final String MASKED_PAN = "maskedPan";
    public static final String MSISDN = "msisdn";
    public static final String PAYMENT_PRODUCT_CC = "paymentProduct";
    public static final String TRANSACTION_STATUS = "transactionStatus";
    public static final String ASPSP_SCA_APPROACH = "ASPSP-SCA-Approach";
    public static final String AMOUNT = "amount";
    public static final String START_DATE = "startDate";
    public static final String FREQUENCY = "frequency";
    public static final String END_DATE = "endDate";
    public static final String EXECUTION_RULE = "executionRule";
    public static final String DEBTOR_ACCOUNT = "debtorAccount";
    public static final String CREDITOR_ACCOUNT = "creditorAccount";
    public static final String CREDITOR_NAME = "creditorName";
    public static final String CREDITOR_AGENT = "creditorAgent";
    public static final String END_TO_END_IDENTIFICATION = "endToEndIdentification";
    public static final String REMITTANCE_INFO_UNSTRUCTURED = "remittanceInformationUnstructured";
    public static final List<String> SUPPORTED_PERIODIC_PAYMENT_FREQUENCY_CODES = Collections
            .unmodifiableList(Arrays.asList(
                    "Daily", "Weekly", "EveryTwoWeeks",
                    "Monthly", "EveryTwoMonths", "Quarterly",
                    "SemiAnnual", "Annual", "MonthlyVariable"
            ));

    // Payload element formats defined by the Berlin Group NextGenPSD2 XS2A Framework 1.3.6 PIS specification
    public static final String CURRENCY_CODE_PATTERN = "[A-Z]{3}";
    public static final String AMOUNT_VALUE_PATTERN = "^-?([0-9]{1,14})(\\.[0-9]{1,3})?$";
    public static final String IBAN_PATTERN = "[A-Z]{2,2}[0-9]{2,2}[a-zA-Z0-9]{1,30}";
    public static final String BBAN_PATTERN = "[a-zA-Z0-9]{1,30}";
    public static final String BICFI_PATTERN = "[A-Z]{6,6}[A-Z2-9][A-NP-Z0-9]([A-Z0-9]{3,3}){0,1}";
    public static final String COUNTRY_CODE_PATTERN = "[A-Z]{2}";
    public static final String DATE_TIME_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([Zz]|[+-]\\d{2}:\\d{2})$";
    public static final int MAX_CREDITOR_NAME_LENGTH = 70;
    public static final int MAX_CREDITOR_AGENT_NAME_LENGTH = 70;
    public static final int MAX_STREET_NAME_LENGTH = 70;
    public static final int MAX_END_TO_END_IDENTIFICATION_LENGTH = 35;
    public static final int MAX_REMITTANCE_INFO_UNSTRUCTURED_LENGTH = 140;
    public static final int MAX_PAN_LENGTH = 35;
    public static final int MAX_MASKED_PAN_LENGTH = 35;
    public static final int MAX_MSISDN_LENGTH = 35;

    public static final String REQUESTED_EXECUTION_DATE = "requestedExecutionDate";
    public static final String REQUESTED_EXECUTION_TIME = "requestedExecutionTime";

    // Elements that a bulk payment entry must not carry, as per the bulk entry definition in the PIS specification
    public static final List<String> FORBIDDEN_BULK_ENTRY_ELEMENTS = Collections.unmodifiableList(Arrays.asList(
            DEBTOR_ACCOUNT, REQUESTED_EXECUTION_DATE, REQUESTED_EXECUTION_TIME
    ));
    // Payment services
    public static final String PAYMENTS = "payments";
    public static final String BULK_PAYMENTS = "bulk-payments";
    public static final String PERIODIC_PAYMENTS = "periodic-payments";
    // Constants that are used by funds confirmation service
    public static final String FUNDS_CONFIRMATIONS_SERVICE_PATH = "confirmation-of-funds";
    // Response related constants
    public static final String SELF = "self";
    public static final String STATUS = "status";
    public static final String SCA_OAUTH = "scaOAuth";
    public static final String SCA_STATUS = "scaStatus";
    public static final String SELECT_AUTH_METHOD = "selectAuthenticationMethod";
}
