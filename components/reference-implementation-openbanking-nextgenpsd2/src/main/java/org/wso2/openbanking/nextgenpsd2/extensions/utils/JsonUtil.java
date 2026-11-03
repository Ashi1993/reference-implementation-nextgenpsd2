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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;

import javax.ws.rs.core.Response;

/**
 * Utility class for reading and writing the JSON the extension endpoints exchange with the accelerator.
 * <p>
 * Every conversion goes through the object mapper configured here, so that a payload is read and written the
 * same way wherever it is handled, most notably the dates carried by the consent payloads.
 */
public final class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtil() {
    }

    /**
     * Convert an object to a JSON object.
     *
     * @param object json convertible object
     * @return JSONObject of passed object
     * @throws ExtensionException if the passed object cannot be converted to JSON
     */
    public static JSONObject convertObjectToJson(Object object) throws ExtensionException {
        try {
            // Convert Object to JSON string, then parse the JSON string to a JSONObject
            return new JSONObject(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException | JSONException e) {
            // The failure of the parser is reported through the cause rather than to the client
            throw new ExtensionException(Response.Status.BAD_REQUEST, ErrorConstants.INVALID_REQUEST,
                    ErrorConstants.JSON_PARSE_ERROR, e);
        }
    }

    /**
     * Serializes an object to JSON, honouring the Jackson annotations its model carries.
     * <p>
     * Response models must be serialized through this method rather than through {@link JSONObject}, since
     * only the object mapper honours those annotations.
     *
     * @param object object to serialize, such as a built response
     * @return the object serialized to a JSON string
     * @throws ExtensionException if the object cannot be serialized
     */
    public static String serializeToJsonString(Object object) throws ExtensionException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ExtensionException(Response.Status.INTERNAL_SERVER_ERROR, ErrorConstants.SERVER_ERROR,
                    ErrorConstants.RESPONSE_SERIALIZATION_FAILED, e);
        }
    }

    /**
     * Reads a JSON string as the given model.
     *
     * @param jsonPayload JSON string to read
     * @param modelClass  class of the model to read the JSON as
     * @param <T>         type of the model to read the JSON as
     * @return the JSON read as the model
     * @throws JsonProcessingException if the JSON cannot be read as the model, which the caller reports as the
     *                                 failure of the payload it was reading
     */
    public static <T> T readValue(String jsonPayload, Class<T> modelClass) throws JsonProcessingException {
        return objectMapper.readValue(jsonPayload, modelClass);
    }

    /**
     * Converts an object that was read as JSON, such as a stored consent receipt, to the given model.
     *
     * @param object     object to convert
     * @param modelClass class of the model to convert the object to
     * @param <T>        type of the model to convert the object to
     * @return the object converted to the model
     */
    public static <T> T convertValue(Object object, Class<T> modelClass) {
        return objectMapper.convertValue(object, modelClass);
    }
}
