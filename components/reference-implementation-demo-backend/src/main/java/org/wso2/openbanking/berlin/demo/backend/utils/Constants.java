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

/**
 * Constant values.
 */
public final class Constants {

    private Constants() {

    }

    /**
     * AccountService constants class
     */
    public static final class AccountService {

        public static final String VALID = "valid";
        public static final String MESSAGE = "message";
        public static final String CODE = "code";
        public static final String REQUEST_ID = "X-Request-ID";
        public static final String BOOKED = "booked";
        public static final String PENDING = "pending";
        public static final String BOTH = "both";

        // Elements of the consent carried by the Account-Request-Information header
        public static final String ACCOUNTS = "accounts";
        public static final String IBAN = "iban";
        public static final String CURRENCY = "currency";
        public static final String RECEIPT = "receipt";
        public static final String ACCESS = "access";
        public static final String CONSENT_MAPPING_RESOURCES = "consentMappingResources";
        public static final String MAPPING_STATUS = "mappingStatus";
        public static final String ACTIVE = "active";
        public static final String ACCOUNT_ID = "account_id";
        public static final String PERMISSION = "permission";

        // Separators used within the account identifiers of the consent mapping resources, e.g. "iban:DE12:USD"
        public static final String ACCOUNT_ID_SEPARATOR = ":";
        public static final String ACCOUNT_KEY_SEPARATOR = "|";

        // Matches any currency of an account reference that was permitted without a currency
        public static final String ANY_CURRENCY = "*";

        public static final String ACCOUNT_NOT_IN_CONSENT = "The requested account is not granted access to by the " +
                "consent";

        private AccountService() {

        }
    }
}
