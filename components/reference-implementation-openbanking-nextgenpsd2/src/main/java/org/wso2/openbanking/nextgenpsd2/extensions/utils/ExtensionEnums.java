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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Class encapsulating all nextGenPSD2 specification specified enums.
 */
public final class ExtensionEnums {

    private ExtensionEnums() {
    }

    /**
     * Returns the constant of an enum that carries the given value, which is the value the constants of this
     * class report through their toString.
     *
     * @param enumType class of the enum to read the value as
     * @param value    value to read
     * @param <T>      type of the enum to read the value as
     * @return the constant carrying the value
     * @throws IllegalArgumentException if the value is blank or is not carried by a constant of the enum
     */
    private static <T extends Enum<T>> T fromValue(Class<T> enumType, String value) {

        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty");
        }

        for (T constant : enumType.getEnumConstants()) {
            if (constant.toString().equals(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    /**
     * Auth Type enum.
     */
    public enum AuthTypeEnum {

        AUTHORISATION("authorisation"),
        CANCELLATION("cancellation");

        private final String value;

        AuthTypeEnum(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static AuthTypeEnum fromValue(String text) {
            return ExtensionEnums.fromValue(AuthTypeEnum.class, text);
        }

    }

    /**
     * Access method enum.
     */
    public enum AccessMethodEnum {

        ACCOUNTS("accounts"),
        BALANCES("balances"),
        TRANSACTIONS("transactions");

        private final String value;

        AccessMethodEnum(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static AccessMethodEnum fromValue(String text) {
            return ExtensionEnums.fromValue(AccessMethodEnum.class, text);
        }

    }

    /**
     * Consent Status enum.
     */
    public enum ConsentStatusEnum {

        RECEIVED("received"),
        REJECTED("rejected"),
        PARTIALLY_AUTHORISED("partiallyAuthorised"),
        VALID("valid"),
        REVOKED_BY_PSU("revokedByPsu"),
        EXPIRED("expired"),
        TERMINATED_BY_TPP("terminatedByTpp");

        private final String value;

        ConsentStatusEnum(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static ConsentStatusEnum fromValue(String text) {
            return ExtensionEnums.fromValue(ConsentStatusEnum.class, text);
        }

    }

    /**
     * Consent Type enum.
     */
    public enum ConsentTypeEnum {

        ACCOUNTS("accounts"),
        PAYMENTS("payments"),
        BULK_PAYMENTS("bulk-payments"),
        PERIODIC_PAYMENTS("periodic-payments"),
        FUNDS_CONFIRMATION("funds-confirmations");

        /**
         * The consent types a payment is initiated under. A payment consent carries a transaction status
         * rather than a consent status, and is addressed on the path of its payment service.
         */
        private static final Set<ConsentTypeEnum> PAYMENT_TYPES =
                Collections.unmodifiableSet(EnumSet.of(PAYMENTS, BULK_PAYMENTS, PERIODIC_PAYMENTS));

        private final String value;

        ConsentTypeEnum(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        /**
         * Checks whether a consent type is one of the payment consent types.
         *
         * @param consentType consent type to check
         * @return true if a payment is initiated under the consent type
         */
        public static boolean isPaymentType(String consentType) {

            for (ConsentTypeEnum paymentType : PAYMENT_TYPES) {
                if (paymentType.toString().equals(consentType)) {
                    return true;
                }
            }
            return false;
        }

        public static ConsentTypeEnum fromValue(String text) {
            return ExtensionEnums.fromValue(ConsentTypeEnum.class, text);
        }

    }

    /**
     * Permission enum.
     */
    public enum PermissionEnum {

        AVAILABLE_ACCOUNTS("availableAccounts"),
        AVAILABLE_ACCOUNTS_WITH_BALANCES("availableAccountsWithBalance"),
        ALL_PSD2("allPsd2"),
        BANK_OFFERED("bankOffered"),
        DEDICATED_ACCOUNTS("dedicatedAccounts");

        private final String value;

        PermissionEnum(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static PermissionEnum fromValue(String text) {
            return ExtensionEnums.fromValue(PermissionEnum.class, text);
        }

    }

    /**
     * SCA Approaches enum.
     */
    public enum ScaApproachEnum {

        REDIRECT("REDIRECT"),
        DECOUPLED("DECOUPLED"),
        EMBEDDED("EMBEDDED");

        private final String value;

        ScaApproachEnum(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static ScaApproachEnum fromValue(String text) {
            return ExtensionEnums.fromValue(ScaApproachEnum.class, text);
        }

    }

    /**
     * Sca Status enum.
     */
    public enum ScaStatusEnum {

        RECEIVED("received"),
        PSU_IDENTIFIED("psuIdentified"),
        PSU_AUTHENTICATED("psuAuthenticated"),
        SCA_METHOD_SELECTED("scaMethodSelected"),
        STARTED("started"),
        UNCONFIRMED("unconfirmed"),
        FINALISED("finalised"),
        FAILED("failed"),
        EXEMPTED("exempted");

        private final String value;

        ScaStatusEnum(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static ScaStatusEnum fromValue(String text) {
            return ExtensionEnums.fromValue(ScaStatusEnum.class, text);
        }

    }

    /**
     * Specifies the status of a particular transaction.
     */
    public enum TransactionStatusEnum {

        // Preceding check of technical validation was successful. Customer profile check was also successful.
        ACCP("AcceptedCustomerProfile"),
        // Settlement on the debtor’s account has been completed.
        ACSC("AcceptedSettlementCompleted"),
        // All preceding checks such as technical validation and customer profile were successful and therefore the
        // payment initiation has been accepted for execution.
        ACSP("AcceptedSettlementInProcess"),
        // Authentication and syntactical and semantic validation are successful
        ACTC("AcceptedTechnicalValidation"),
        // Instruction is accepted but a change will be made, such as date or remittance not sent.
        ACWC("AcceptedWithChange"),
        // Payment instruction included in the credit transfer is accepted without being posted to the creditor
        // customer’s account.
        ACWP("AcceptedWithoutPosting"),
        // Payment initiation has been received by the receiving agent.
        RCVD("Received"),
        // Payment initiation or individual transaction included in the payment initiation is pending. Further
        // checks and status update will be performed.
        PDNG("Pending"),
        // Payment initiation or individual transaction included in the payment initiation has been rejected.
        RJCT("Rejected"),
        // PSU has revoked a previously given consent for a particular payment.
        REVOKED("Revoked"),
        // Cancellation of a payment
        CANC("Cancelled"),
        // Partially Accepted Technical Correct in cases of multi level SCA.
        PATC("PartiallyAcceptedTechnicalCorrect");

        private final String value;

        TransactionStatusEnum(String value) {

            this.value = value;
        }

        public static TransactionStatusEnum fromValue(String text) {
            return ExtensionEnums.fromValue(TransactionStatusEnum.class, text);
        }

        public String toString() {

            return String.valueOf(value);
        }
    }
}
