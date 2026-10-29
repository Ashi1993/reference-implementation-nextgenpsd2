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

package org.wso2.openbanking.nextgenpsd2.extensions.model;

import java.util.List;

import javax.validation.Valid;

/**
 * Additional information access object sent within the access object of an account initiation request.
 */
public class AdditionalInformationAccess {

    private List<@Valid AccountReference> ownerName;

    private List<@Valid AccountReference> trustedBeneficiaries;

    public List<AccountReference> getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(List<AccountReference> ownerName) {
        this.ownerName = ownerName;
    }

    public List<AccountReference> getTrustedBeneficiaries() {
        return trustedBeneficiaries;
    }

    public void setTrustedBeneficiaries(List<AccountReference> trustedBeneficiaries) {
        this.trustedBeneficiaries = trustedBeneficiaries;
    }
}
