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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;

import java.util.Optional;

import javax.ws.rs.core.Response;

/**
 * Utility class for reading and validating the headers the accelerator forwards with a request.
 * <p>
 * A header is read without considering the case of its name, since the case a header reaches the extension
 * with depends on the client that sent it.
 */
public final class HeaderUtil {

    private static final Log log = LogFactory.getLog(HeaderUtil.class);

    private HeaderUtil() {
    }

    /**
     * This method checks a provided header key is in the header map without considering the case.
     *
     * @param headersJson headers map
     * @param headerKey header key that need to be checked
     * @return true if present, false otherwise
     */
    public static boolean checkCaseIgnoredHeader(String requestId, JSONObject headersJson, String headerKey) {

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating header: %s", requestId, headerKey));
        }
        for (String header : headersJson.keySet()) {
            if (header.equalsIgnoreCase(headerKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the TPP-Redirect-Preferred request header.
     *
     * @param headersJSON request headers
     * @return if redirect approach preferred or not
     */
    public static Optional<Boolean> isTppRedirectPreferred(String requestId, JSONObject headersJSON) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Determining whether the TPP-Redirect-Preferred header is true or "
                    + "false or not present", requestId));
        }
        if (checkCaseIgnoredHeader(requestId, headersJSON,
                CommonConstants.TPP_REDIRECT_PREFERRED_HEADER)) {
            return Optional.of(Boolean.parseBoolean(headersJSON
                    .getString(CommonConstants.TPP_REDIRECT_PREFERRED_HEADER)));
        }

        return Optional.empty();
    }

    public static void validateRequestIdentificationHeader(Object headers)
            throws ValidationFailureException, ExtensionException {
        // To allow idempotency header validation it needs to be forwarded
        // Therefore X-Request-ID needs to be added to the configuration in the IS deployment.toml
        // [financial_services.consent.manage_extension]
        // allowed_headers = ["X-Request-ID"]

        // Assuming the accelerator only forwards (even empty) JSON object
        JSONObject headersJSON = JsonUtil.convertObjectToJson(headers);

        if (!headersJSON.has(CommonConstants.X_REQUEST_ID_HEADER)) {
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.X_REQUEST_ID_MISSING));
        }

        if (!CommonConsentValidationUtil.isValidUuid(headersJSON.getString(CommonConstants.X_REQUEST_ID_HEADER))) {
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.X_REQUEST_ID_INVALID));
        }
    }

    /**
     * Reads the client the request was made by from the headers the accelerator forwards.
     *
     * @param headers request headers of the request
     * @return the client ID of the requesting client
     * @throws ExtensionException if the headers carry no client ID
     */
    public static String getClientId(JSONObject headers) throws ExtensionException {
        try {
            return headers.getString(CommonConstants.X_WSO2_CLIENT_ID_KEY);
        } catch (JSONException e) {
            throw new ExtensionException(Response.Status.BAD_REQUEST, ErrorConstants.INVALID_REQUEST,
                    ErrorConstants.CLIENT_ID_NOT_FOUND, e);
        }
    }

    /**
     * Reads the X-Request-ID header from the request headers sent by the accelerator.
     *
     * @param headers request headers of the request
     * @return value of the X-Request-ID header, or an empty string if it is absent
     * @throws ExtensionException if the request headers cannot be read as JSON
     */
    public static String getXRequestId(Object headers) throws ExtensionException {

        return JsonUtil.convertObjectToJson(headers).optString(CommonConstants.X_REQUEST_ID_HEADER);
    }

    /**
     * Validates the PSU-IP-Address request header.
     *
     * @param headers request headers
     */
    public static void validatePsuIpAddress(String requestId, JSONObject headers)
            throws ValidationFailureException, ExtensionException {

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating PSU-IP-Address header", requestId));
        }
        if (headers.has(CommonConstants.PSU_IP_ADDRESS_HEADER)) {
            String psuIpAddress = headers.getString(CommonConstants.PSU_IP_ADDRESS_HEADER);

            if (StringUtils.isEmpty(psuIpAddress)) {
                throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                        ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                                TPPMessage.CodeEnum.FORMAT_ERROR, String.format("Invalid %s header",
                                        CommonConstants.PSU_IP_ADDRESS_PROPER_CASE_HEADER)
                ));
            }
        } else {
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, ErrorConstants.PSU_IP_ADDRESS_MISSING));
        }
    }

    /**
     * Validates the TPP-Redirect-Preferred request header.
     *
     * @param headers request headers
     */
    public static void validateTppRedirectPreferredHeader(String requestId, JSONObject headers)
            throws ValidationFailureException, ExtensionException {

        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Validating TPP-Redirect-Preferred header according to the "
                    + "specification", requestId));
        }
        Optional<Boolean> isRedirectPreferred = isTppRedirectPreferred(requestId, headers);

        if ((isRedirectPreferred.isPresent() && BooleanUtils.isTrue(isRedirectPreferred.get()))
                && !ScaUtil.getScaApproach(ExtensionEnums.ScaApproachEnum.REDIRECT).isPresent()) {
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, String.format("%s SCA Approach is not supported",
                                    ExtensionEnums.ScaApproachEnum.REDIRECT)));
        }

        if ((isRedirectPreferred.isPresent() && BooleanUtils.isFalse(isRedirectPreferred.get()))
                && !ScaUtil.getScaApproach(ExtensionEnums.ScaApproachEnum.DECOUPLED).isPresent()) {

            //ToDo: Since decoupled approach is not supported yet, an error is thrown if the redirect header is false.
            //issue: https://github.com/wso2-enterprise/financial-open-banking/issues/6858
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError("headers", TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.FORMAT_ERROR, String.format("%s SCA Approach is not supported",
                                    ExtensionEnums.ScaApproachEnum.DECOUPLED)));
        }
    }

    public static JSONObject getIdempotencyHeaderJSON(String xRequestID) {
        JSONObject idempotencyHeader = new JSONObject();
        idempotencyHeader.put(CommonConstants.X_REQUEST_ID_PROPER_CASE_HEADER, xRequestID);
        return idempotencyHeader;
    }
}
