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

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.utils.CommonConstants;

import java.util.Map;

/**
 * Context of a data access request, read from the fields the accelerator sends to the validate-consent-access
 * endpoint beyond its declared schema.
 * <p>
 * The v1.0.3 extensions contract declares only the consent ID, the consent resource and the data request payload
 * for this operation, while the accelerator additionally sends the request headers, the resource parameters, the
 * client ID and the user ID. Every accessor here reports whether its value was actually sent, so the validations
 * that depend on it can be skipped on a deployment that sends the declared payload only.
 */
public class ConsentAccessContext {

    private final JSONObject headers;
    private final JSONObject resourceParams;
    private final String clientId;
    private final String userId;
    private final String electedResource;

    public ConsentAccessContext(Map<String, Object> additionalProperties) {

        this.headers = readObject(additionalProperties, CommonConstants.HEADERS_KEY);
        this.resourceParams = readObject(additionalProperties, CommonConstants.RESOURCE_PARAMS_KEY);
        this.clientId = readString(additionalProperties, CommonConstants.CLIENT_ID_KEY);
        this.userId = readString(additionalProperties, CommonConstants.USER_ID_KEY);
        this.electedResource = readString(additionalProperties, CommonConstants.ELECTED_RESOURCE_KEY);
    }

    /**
     * Whether the accelerator sent the data request context at all.
     *
     * @return true if any part of the context is present
     */
    public boolean isPresent() {

        return headers != null || resourceParams != null || clientId != null || userId != null;
    }

    public boolean hasHeaders() {

        return headers != null;
    }

    public JSONObject getHeaders() {

        return headers == null ? new JSONObject() : new JSONObject(headers.toString());
    }

    /**
     * Reads a request header without considering the case of its name.
     *
     * @param headerName name of the header to read
     * @return the header value, or null if the header was not sent
     */
    public String getHeader(String headerName) {

        if (headers == null) {
            return null;
        }
        for (String header : headers.keySet()) {
            if (header.equalsIgnoreCase(headerName)) {
                return headers.optString(header, null);
            }
        }
        return null;
    }

    public boolean hasResourcePath() {

        return StringUtils.isNotBlank(getResourcePath());
    }

    /**
     * Path of the resource the TPP is accessing, such as /accounts/{account-id}/balances.
     *
     * @return the resource path, or null if it was not sent
     */
    public String getResourcePath() {

        return resourceParams == null ? null : resourceParams.optString(CommonConstants.RESOURCE_KEY, null);
    }

    public boolean hasHttpMethod() {

        return StringUtils.isNotBlank(getHttpMethod());
    }

    /**
     * HTTP method of the data access request.
     *
     * @return the HTTP method, or null if it was not sent
     */
    public String getHttpMethod() {

        return resourceParams == null ? null : resourceParams.optString(CommonConstants.HTTP_METHOD_KEY, null);
    }

    /**
     * Context of the API the request was made against.
     *
     * @return the API context, or null if it was not sent
     */
    public String getContext() {

        return resourceParams == null ? null : resourceParams.optString(CommonConstants.CONTEXT_KEY, null);
    }

    /**
     * Client ID of the token the data access request was made with.
     *
     * @return the client ID, or null if it was not sent
     */
    public String getClientId() {

        return clientId;
    }

    /**
     * User ID of the token the data access request was made with, with any appended tenant domain removed.
     *
     * @return the user ID, or null if it was not sent
     */
    public String getUserId() {

        if (userId == null) {
            return null;
        }
        String tenantAwareUserId = userId;
        while (StringUtils.endsWithIgnoreCase(tenantAwareUserId, CommonConstants.SUPER_TENANT_DOMAIN)) {
            tenantAwareUserId = StringUtils.removeEndIgnoreCase(tenantAwareUserId,
                    CommonConstants.SUPER_TENANT_DOMAIN);
        }
        return tenantAwareUserId;
    }

    /**
     * Templated resource the request was matched to, such as /accounts/{account-id}.
     *
     * @return the elected resource, or null if it was not sent
     */
    public String getElectedResource() {

        return electedResource;
    }

    private static JSONObject readObject(Map<String, Object> additionalProperties, String key) {

        Object value = additionalProperties == null ? null : additionalProperties.get(key);
        if (value instanceof Map) {
            return new JSONObject((Map<?, ?>) value);
        }
        if (value instanceof String && StringUtils.isNotBlank((String) value)) {
            return new JSONObject((String) value);
        }
        return null;
    }

    private static String readString(Map<String, Object> additionalProperties, String key) {

        Object value = additionalProperties == null ? null : additionalProperties.get(key);
        return value instanceof String && StringUtils.isNotBlank((String) value) ? (String) value : null;
    }
}
