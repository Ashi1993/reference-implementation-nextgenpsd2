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

package org.wso2.openbanking.nextgenpsd2.extensions.exceptions;

import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.ErrorUtil;

import javax.ws.rs.core.Response;

/**
 * Exception class for success responses carrying FailedResponse objects.
 * <p>
 * A validation failure is reported to the accelerator as a successful call carrying an error payload, so the
 * status it carries is the status of that payload rather than of the response it is returned in.
 */
public class ValidationFailureException extends Exception {

    private final Response.Status errorStatus;
    private final JSONObject data;

    public ValidationFailureException(Response.Status errorStatus, JSONObject data) {
        super(ErrorUtil.getErrorMessage(data));
        this.errorStatus = errorStatus;
        this.data = data;
    }

    public ValidationFailureException(Response.Status errorStatus, JSONObject data, Throwable e) {
        super(ErrorUtil.getErrorMessage(data), e);
        this.errorStatus = errorStatus;
        this.data = data;
    }

    /**
     * Getter for the status the validation failure is reported under.
     *
     * @return status of the validation failure
     */
    public Response.Status getStatus() {
        return this.errorStatus;
    }

    /**
     * Format error to the expected FailedResponse object format.
     * @return formatted error message as a JSONObject
     */
    public JSONObject toJson() {
        return ErrorUtil.getFormattedFailedResponse(this.errorStatus.getStatusCode(), this.data);
    }

    /**
     * Return formatted error as string.
     * @return formatted error message as a string
     */
    public String toJsonString() {
        return toJson().toString();
    }
}
