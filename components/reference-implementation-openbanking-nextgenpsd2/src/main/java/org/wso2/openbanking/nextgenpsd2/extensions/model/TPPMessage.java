/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.openbanking.nextgenpsd2.extensions.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

/**
 * TPPMessage class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TPPMessage {


    /**
     * Error category enum.
     */
    public enum CategoryEnum {
        ERROR, WARNING
    };
    @NotNull
    private CategoryEnum category = null;

    /**
     * Error Code Enum.
     */
    public enum CodeEnum {

        // Service unspecific HTTP error codes
        CERTIFICATE_INVALID("CERTIFICATE_INVALID", "401"),
        CERTIFICATE_EXPIRED("CERTIFICATE_EXPIRED", "401"),
        CERTIFICATE_BLOCKED("CERTIFICATE_BLOCKED", "401"),
        CERTIFICATE_REVOKED("CERTIFICATE_REVOKED", "401"),
        CERTIFICATE_MISSING("CERTIFICATE_MISSING", "401"),
        SIGNATURE_INVALID("SIGNATURE_INVALID", "401"),
        SIGNATURE_MISSING("SIGNATURE_MISSING", "401"),
        FORMAT_ERROR("FORMAT_ERROR", "400"),
        PARAMETER_NOT_CONSISTENT("PARAMETER_NOT_CONSISTENT", "400"),
        PSU_CREDENTIALS_INVALID("PSU_CREDENTIALS_INVALID", "401"),
        SERVICE_INVALID("SERVICE_INVALID", "400"),
        SERVICE_INVALID_405("SERVICE_INVALID", "405"),
        SERVICE_BLOCKED("SERVICE_BLOCKED", "403"),
        CORPORATE_ID_INVALID("CORPORATE_ID_INVALID", "401"),
        CONSENT_UNKNOWN_403("CONSENT_UNKNOWN", "403"),
        CONSENT_UNKNOWN("CONSENT_UNKNOWN", "400"),
        CONSENT_INVALID("CONSENT_INVALID", "401"),
        CONSENT_EXPIRED("CONSENT_EXPIRED", "401"),
        TOKEN_UNKNOWN("TOKEN_UNKNOWN", "401"),
        TOKEN_INVALID("TOKEN_INVALID", "401"),
        TOKEN_EXPIRED("TOKEN_EXPIRED", "401"),
        RESOURCE_UNKNOWN("RESOURCE_UNKNOWN", "400"),
        RESOURCE_UNKNOWN_403("RESOURCE_UNKNOWN", "403"),
        RESOURCE_UNKNOWN_404("RESOURCE_UNKNOWN", "404"),
        RESOURCE_EXPIRED("RESOURCE_EXPIRED", "400"),
        RESOURCE_EXPIRED_403("RESOURCE_EXPIRED", "403"),
        ROLE_INVALID("ROLE_INVALID", "401"),
        TIMESTAMP_INVALID("TIMESTAMP_INVALID", "400"),
        PERIOD_INVALID("PERIOD_INVALID", "400"),
        SCA_METHOD_UNKNOWN("SCA_METHOD_UNKNOWN", "400"),
        SESSIONS_NOT_SUPPORTED("SESSIONS_NOT_SUPPORTED", "400"),
        ACCESS_EXCEEDED("ACCESS_EXCEEDED", "429"),
        REQUESTED_FORMATS_INVALID("REQUESTED_FORMATS_INVALID", "406"),
        INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "500"),

        // PIS specific HTTP error codes
        PRODUCT_INVALID("PRODUCT_INVALID", "403"),
        PRODUCT_UNKNOWN("PRODUCT_UNKNOWN", "404"),
        PAYMENT_FAILED("PAYMENT_FAILED", "400"),
        REQUIRED_KID_MISSING("REQUIRED_KID_MISSING", "401"),
        EXECUTION_DATE_INVALID("EXECUTION_DATE_INVALID", "400"),
        CANCELLATION_INVALID("CANCELLATION_INVALID", "405");

        private String value;
        private String statusCode;

        CodeEnum(String value, String statusCode) {
            this.value = value;
            this.statusCode = statusCode;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        public String getStatusCode() {

            return statusCode;
        }

        @JsonCreator
        public static CodeEnum fromValue(String text) {

            if (StringUtils.isBlank(text)) {
                throw new IllegalArgumentException("Value cannot be null or empty");
            }

            for (CodeEnum b : CodeEnum.values()) {
                if (text.equals(String.valueOf(b.value))) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unknown value: " + text);
        }
    }

    private CodeEnum code = null;

    private String text = null;

    private String path = null;
    /**
     **/
    @JsonProperty("category")
    public CategoryEnum getCategory() {
        return category;
    }
    public void setCategory(CategoryEnum category) {
        this.category = category;
    }


    /**
     **/
    @JsonProperty("code")
    public CodeEnum getCode() {
        return code;
    }
    public void setCode(CodeEnum code) {
        this.code = code;
    }


    /**
     **/
    @JsonProperty("text")
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    /**
     **/
    @JsonProperty("path")
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();
        sb.append("class TppMessageDTO {\n");

        sb.append("  category: ").append(category).append("\n");
        sb.append("  code: ").append(code).append("\n");
        sb.append("  text: ").append(text).append("\n");
        sb.append("  path: ").append(path).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
