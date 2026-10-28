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

package org.wso2.openbanking.nextgenpsd2.extensions.impl.oauth;

import org.wso2.openbanking.nextgenpsd2.extensions.model.token.IssueRefreshTokenRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.token.SuccessResponseIssueRefreshToken;
import org.wso2.openbanking.nextgenpsd2.extensions.model.token.SuccessResponseIssueRefreshTokenData;

import javax.ws.rs.core.Response;

/**
 * Implementation class for issuing the refresh token.
 */
public class IssueRefreshTokenApiImpl {

    /**
     * Handles issuing the refresh token.
     *
     * @param issueRefreshTokenRequestBody the IssueRefreshTokenApi request body
     * @return the built response
     */
    public static Response handleIssueRefreshToken(IssueRefreshTokenRequestBody issueRefreshTokenRequestBody) {

        SuccessResponseIssueRefreshToken successResponseIssueRefreshToken = new SuccessResponseIssueRefreshToken();
        successResponseIssueRefreshToken.setResponseId(issueRefreshTokenRequestBody.getRequestId());
        successResponseIssueRefreshToken.setStatus(SuccessResponseIssueRefreshToken.StatusEnum.SUCCESS);

        SuccessResponseIssueRefreshTokenData successResponseIssueRefreshTokenData =
                new SuccessResponseIssueRefreshTokenData();
        successResponseIssueRefreshTokenData.setIssueRefreshToken(true);

        long consentValidityPeriod = issueRefreshTokenRequestBody.getData().getConsentValidityPeriod();
        long consentCreatedPeriod = issueRefreshTokenRequestBody.getData().getConsentCreatedTime();

        successResponseIssueRefreshTokenData.setRefreshTokenValidityPeriod(consentValidityPeriod -
                consentCreatedPeriod);
        successResponseIssueRefreshToken.setData(successResponseIssueRefreshTokenData);
        return Response.ok().entity(successResponseIssueRefreshToken).build();
    }
}
