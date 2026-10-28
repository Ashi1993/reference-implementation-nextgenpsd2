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

package org.wso2.openbanking.nextgenpsd2.extensions.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

/**
 * Data retrieval util class.
 */
public class DataRetrievalUtil {

    private static final Log log = LogFactory.getLog(DataRetrievalUtil.class);

    /**
     * Returns the account list string after fetching from the bank backend.
     *
     * @param accountsURL shareable accounts retrieval URL
     * @param parameters  URL params
     * @param headers     request headers
     * @return retrieved accounts string from endpoint
     *
     */
    public static String getAccountsFromEndpoint(String accountsURL, Map<String, String> parameters,
                                                 Map<String, String> headers) {

        String retrieveUrl = accountsURL.endsWith("/") ? accountsURL : accountsURL + "/";
        if (!parameters.isEmpty()) {
            retrieveUrl = buildRequestURL(retrieveUrl, parameters);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Accounts retrieve endpoint : %s", retrieveUrl));
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = buildHttpGetRequest(retrieveUrl, headers);
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                log.error("Retrieving sharable accounts failed. Status code: " +
                        response.getStatusLine().getStatusCode());
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {

                StringBuilder buffer = new StringBuilder();
                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    buffer.append(inputLine);
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Sharable accounts endpoint returned : %s", buffer));
                }

                return buffer.toString();
            }

        } catch (IOException e) {
            log.error("Exception occurred while retrieving sharable accounts", e);
        }

        return null;
    }

    /**
     * Builds an http get request to retrieve data from external endpoints.
     *
     * @param url url for the request
     * @param headers headers send with the request
     * @return http get request object
     */
    private static HttpGet buildHttpGetRequest(String url, Map<String, String> headers) {
        HttpGet request = new HttpGet(url);
        request.addHeader(HttpHeaders.ACCEPT, "application/json");

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    request.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }

        return request;
    }

    /**
     * Build the complete URL with query parameters sent in the map.
     *
     * @param baseURL    the base URL
     * @param parameters map of parameters
     * @return the output URL
     */
    private static String buildRequestURL(String baseURL, Map<String, String> parameters) {

        List<NameValuePair> pairs = new ArrayList<>();

        for (Map.Entry<String, String> key : parameters.entrySet()) {
            if (key.getKey() != null && key.getValue() != null) {
                pairs.add(new BasicNameValuePair(key.getKey(), key.getValue()));
            }
        }
        String queries = URLEncodedUtils.format(pairs, StandardCharsets.UTF_8);
        return baseURL + "?" + queries;
    }

    /**
     * Get accounts array from endpoint.
     *
     * @param userId      user id
     * @param accountsURL accounts base URL
     * @param parameters  URL parameters
     * @param headers     request headers
     * @return list of accounts
     */
    public static JSONArray getAccountsFromEndpoint(String userId, String accountsURL,
                                                        Map<String, String> parameters, Map<String, String> headers) {

        if (!accountsURL.endsWith("/")) {
            accountsURL += "/";
        }
        accountsURL += userId;

        if (log.isDebugEnabled()) {
            log.debug(String.format("Getting accounts details from backend endpoint %s", accountsURL));
        }
        String accountData = DataRetrievalUtil.getAccountsFromEndpoint(accountsURL, parameters, headers);
        if (accountData == null) {
            log.error("No account details available");
            return null;
        }

        JSONArray accountList = new JSONArray();
        try {
            JSONObject accountJson = new JSONObject(accountData);

            JSONArray slideContent = (JSONArray) accountJson.get("accounts");

            for (Object o : slideContent) {
                JSONObject slide = (JSONObject) o;
                String accountRefType = CommonConsentValidationUtil.getAccountReferenceType(slide);

                if (accountRefType == null) {
                    continue;
                }

                String accountId = (String) slide.get(accountRefType);

                JSONObject accountRefObject = new JSONObject();
                accountRefObject.put(accountRefType, accountId);
                accountRefObject.put(CommonConstants.CURRENCY,
                        slide.get(CommonConstants.CURRENCY));
                accountRefObject.put(CommonConstants.IS_DEFAULT,
                        slide.get(CommonConstants.IS_DEFAULT));

                accountList.put(accountRefObject);
            }

            // Remove currency information from non multi-currency accounts
            return removeCurrencyInfoFromSingleCurrencyAccounts(accountList);
        } catch (JSONException e) {
            log.error(ErrorConstants.JSON_PARSE_ERROR, e);
            return null;
        }
    }

    /**
     * Returns the accounts array provided after removing the currency information from single currency accounts.
     *
     * @param accountRefArray JSON array containing account info from bank back end
     * @return accounts array without currency information for single currency accounts
     */
    private static JSONArray removeCurrencyInfoFromSingleCurrencyAccounts(JSONArray accountRefArray) {

        for (Object accountObject : accountRefArray) {
            JSONObject accountJSON = (JSONObject) accountObject;
            if (ConsentAuthorizationUtil
                    .getFilteredAccountsForAccountNumber(accountJSON, accountRefArray).length() == 1) {
                accountJSON.remove(CommonConstants.CURRENCY);
            }
        }

        return accountRefArray;
    }
}
