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

package org.wso2.openbanking.nextgenpsd2.extensions.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;
import org.wso2.openbanking.nextgenpsd2.extensions.validator.consent.ValidAccountAccess;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

/**
 * Account access object sent with account initiation request.
 */
@ValidAccountAccess
public class AccountAccess {

    /**
     * Access scope values for availableAccounts, availableAccountsWithBalance
     * or allPsd2.
     */
    public enum AccessScope {
        ALL_ACCOUNTS(CommonConstants.ALL_ACCOUNTS),
        ALL_ACCOUNTS_WITH_OWNER_NAME(CommonConstants.ALL_ACCOUNTS_WITH_OWNER_NAME);

        private final String value;

        AccessScope(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static AccessScope fromValue(String value) {
            for (AccessScope scope : values()) {
                if (scope.value.equalsIgnoreCase(value)) {
                    return scope;
                }
            }
            throw new IllegalArgumentException(
                    "Invalid value for '" + value + "'. Expected '"
                            + CommonConstants.ALL_ACCOUNTS + "' or '"
                            + CommonConstants.ALL_ACCOUNTS_WITH_OWNER_NAME + "'.");
        }
    }

    private List<@Valid AccountReference> accounts;
    private List<@Valid AccountReference> balances;
    private List<@Valid AccountReference> transactions;

    private AccessScope availableAccounts;
    private AccessScope availableAccountsWithBalances;
    private AccessScope allPsd2;

    private Map<String, Object> additionalInformation;

    public List<AccountReference> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountReference> accounts) {
        this.accounts = accounts;
    }

    public List<AccountReference> getBalances() {
        return balances;
    }

    public void setBalances(List<AccountReference> balances) {
        this.balances = balances;
    }

    public List<AccountReference> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<AccountReference> transactions) {
        this.transactions = transactions;
    }

    public AccessScope getAvailableAccounts() {
        return availableAccounts;
    }

    public void setAvailableAccounts(AccessScope availableAccounts) {
        this.availableAccounts = availableAccounts;
    }

    public AccessScope getAvailableAccountsWithBalances() {
        return availableAccountsWithBalances;
    }

    public void setAvailableAccountsWithBalances(AccessScope availableAccountsWithBalances) {
        this.availableAccountsWithBalances = availableAccountsWithBalances;
    }

    public AccessScope getAllPsd2() {
        return allPsd2;
    }

    public void setAllPsd2(AccessScope allPsd2) {
        this.allPsd2 = allPsd2;
    }

    public Map<String, Object> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Map<String, Object> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }
}
