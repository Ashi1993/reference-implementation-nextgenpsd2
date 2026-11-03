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

package org.wso2.openbanking.nextgenpsd2.extensions.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;

import javax.ws.rs.core.Response;

/**
 * Utility class for building the responses returned by the extension endpoints.
 */
public final class ExtensionResponseUtil {

    private ExtensionResponseUtil() {
    }

    /**
     * Builds the response returned when an extension request is handled successfully.
     * <p>
     * The payload is serialized with {@link JSONObject}. A payload whose model carries Jackson annotations
     * must be built with {@link #buildSerializedSuccessResponse(Object)} instead, since the two serializers
     * do not produce the same JSON and are therefore not interchangeable.
     *
     * @param responsePayload response payload produced by the handler
     * @return built success response
     */
    public static Response buildSuccessResponse(Object responsePayload) {
        return Response.ok().entity(new JSONObject(responsePayload).toString()).build();
    }

    /**
     * Builds the response returned when an extension request is handled successfully, serializing the
     * payload with the configured object mapper so that its Jackson annotations are honoured.
     *
     * @param responsePayload response payload produced by the handler
     * @return built success response
     * @throws ExtensionException if the response payload cannot be serialized
     */
    public static Response buildSerializedSuccessResponse(Object responsePayload) throws ExtensionException {
        return Response.ok().entity(JsonUtil.serializeToJsonString(responsePayload)).build();
    }

    /**
     * Logs the given error along with the exception that caused it and builds the error response returned
     * for it, so that a failure is reported and answered in one place.
     * <p>
     * The logger of the calling endpoint is passed in, so log entries stay attributed to the endpoint that
     * failed rather than to this class. Validation and authorization failures are reported to the
     * accelerator as successful calls carrying an error payload, so they are built with an OK status.
     *
     * @param log        logger of the endpoint that failed
     * @param errorLog   error to be logged, including the request identifier of the failed request
     * @param e          exception that caused the failure, or null if the failure was not raised as one
     * @param status     status of the response to be built
     * @param error      error payload to be returned to the client
     * @param xRequestId value of the X-Request-ID header of the request, echoed back as a response header,
     *                   or null to omit the header
     * @return built error response
     */
    public static Response buildErrorResponse(Log log, String errorLog, Throwable e, Response.Status status,
                                              Object error, String xRequestId) {
        log.error(errorLog, e);

        Response.ResponseBuilder responseBuilder = Response.status(status).entity(error);

        if (StringUtils.isNotBlank(xRequestId)) {
            responseBuilder.header(CommonConstants.X_REQUEST_ID_PROPER_CASE_HEADER, xRequestId);
        }

        return responseBuilder.build();
    }
}
