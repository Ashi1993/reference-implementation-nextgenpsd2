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

package org.wso2.openbanking.nextgenpsd2.extensions.impl.client;

import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.model.dcr.ClientProcessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.dcr.SuccessResponseClientProcess;
import org.wso2.openbanking.nextgenpsd2.extensions.model.dcr.SuccessResponseClientProcessData;

import javax.ws.rs.core.Response;

/**
 * Implementation class for pre-processing the client retrieval.
 */
public class PreProcessClientRetrievalApiImpl {

    /**
     * Handles pre-processing the client retrieval.
     *
     * @param clientProcessRequestBody the PreProcessClientRetrievalApi request body
     * @return the built response
     */
    public static Response handleDCRClientRetrieval(ClientProcessRequestBody clientProcessRequestBody) {

        SuccessResponseClientProcessData successResponseData = new SuccessResponseClientProcessData();
        successResponseData.setClientData(new JSONObject());

        SuccessResponseClientProcess successResponseDCRValidation = new SuccessResponseClientProcess();

        successResponseDCRValidation.setResponseId(clientProcessRequestBody.getRequestId());
        successResponseDCRValidation.setStatus(SuccessResponseClientProcess.StatusEnum.SUCCESS);
        successResponseDCRValidation.setData(successResponseData);

        return Response.ok().entity(successResponseDCRValidation).build();
    }
}
