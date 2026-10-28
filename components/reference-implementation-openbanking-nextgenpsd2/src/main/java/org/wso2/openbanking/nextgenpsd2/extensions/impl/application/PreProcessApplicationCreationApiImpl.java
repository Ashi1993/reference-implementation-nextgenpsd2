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

package org.wso2.openbanking.nextgenpsd2.extensions.impl.application;

import org.wso2.openbanking.nextgenpsd2.extensions.model.application.AppCreateProcessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.SuccessResponseApplicationCreation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.SuccessResponseApplicationCreationData;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

/**
 * Implementation class for pre-processing the application creation.
 */
public class PreProcessApplicationCreationApiImpl {

    /**
     * Handles pre-processing the application creation.
     *
     * @param appCreateProcessRequestBody the PreProcessApplicationCreationApi request body
     * @return the built response
     */
    public static Response handleMCRApplicationCreation(AppCreateProcessRequestBody appCreateProcessRequestBody) {

        SuccessResponseApplicationCreationData successResponseData = new SuccessResponseApplicationCreationData();
        successResponseData.setClientId("testClientIdToolkit");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("test", "test");
        successResponseData.setAdditionalAppData(additionalData);

        SuccessResponseApplicationCreation successResponse = new SuccessResponseApplicationCreation();
        successResponse.setStatus(SuccessResponseApplicationCreation.StatusEnum.SUCCESS);
        successResponse.setResponseId(appCreateProcessRequestBody.getRequestId());
        successResponse.setData(successResponseData);
        return Response.ok().entity(successResponse).build();
    }
}
