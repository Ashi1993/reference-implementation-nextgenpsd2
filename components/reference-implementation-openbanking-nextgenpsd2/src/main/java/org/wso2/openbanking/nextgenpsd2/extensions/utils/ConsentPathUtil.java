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

/**
 * Utility class for reading the resource path a consent request was made under.
 * <p>
 * A path that is shorter than the element being read is reported as an empty element, so that it is rejected
 * as an unrecognised path rather than failing the request with an index error.
 */
public final class ConsentPathUtil {

    private ConsentPathUtil() {
    }

    /**
     * Used to get the consent type an authorisation request.
     *
     * @param requestPath authorisation request path string
     * @return returns the relative consent type for the request
     */
    public static String getConsentTypeFromRequestPath(String requestPath) {

        String[] pathElements = requestPath.split("/");
        String authorisationConsentType;

        if (pathElements.length > 1) {
            if (CommonConstants.FUNDS_CONFIRMATIONS_SERVICE_PATH.equals(pathElements[1])) {
                authorisationConsentType = pathElements[1];
            } else {
                authorisationConsentType = pathElements[0];
            }
        } else {
            authorisationConsentType = pathElements[0];
        }

        switch (authorisationConsentType) {
            case CommonConstants.PAYMENTS_SERVICE_PATH:
                return ExtensionEnums.ConsentTypeEnum.PAYMENTS.toString();
            case CommonConstants.BULK_PAYMENTS_SERVICE_PATH:
                return ExtensionEnums.ConsentTypeEnum.BULK_PAYMENTS.toString();
            case CommonConstants.PERIODIC_PAYMENTS_SERVICE_PATH:
                return ExtensionEnums.ConsentTypeEnum.PERIODIC_PAYMENTS.toString();
            case CommonConstants.FUNDS_CONFIRMATIONS_SERVICE_PATH:
                return ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION.toString();
            default:
                return ExtensionEnums.ConsentTypeEnum.ACCOUNTS.toString();
        }
    }

    /**
     * Returns the part of the path that differentiates the request path into
     * either Accounts, Payments or Funds confirmations.
     *
     * @param requestPath the request path string
     * @return the part to recognize the consent service related to the request
     */
    public static String getServiceDifferentiatingRequestPath(String requestPath) {

        if (requestPath == null) {
            return "";
        }

        String[] requestPathArray = requestPath.split("/");

        if (StringUtils.contains(requestPath, CommonConstants.EXPLICIT_AUTHORISATION_PATH_END)
                || StringUtils.contains(requestPath,
                CommonConstants.PAYMENT_EXPLICIT_CANCELLATION_AUTHORISATION_PATH_END)) {
            /*
            Example request paths applicable here:
            1) consents/{consentId}/authorisations
            2) {payment-service}/{payment-product}/{paymentId}/cancellation-authorisations
            3) {payment-service}/{payment-product}/{paymentId}/authorisations
            4) consents/confirmation-of-funds/{consentId}/authorisations
             */
            if (StringUtils.equals(CommonConstants.ACCOUNTS_CONSENT_PATH, pathElementAt(requestPathArray, 0))
                    && !StringUtils.equals(CommonConstants.FUNDS_CONFIRMATIONS_SERVICE_PATH,
                    pathElementAt(requestPathArray, 1))) {
                /*
                Example request paths applicable here:
                1) consents/{consentId}/authorisations
                 */
                return pathElementAt(requestPathArray, 2);
            }
            /*
            Example request paths applicable here:
            1) {payment-service}/{payment-product}/{paymentId}/cancellation-authorisations
            2) {payment-service}/{payment-product}/{paymentId}/authorisations
            3) consents/confirmation-of-funds/{consentId}/authorisations
             */
            return pathElementAt(requestPathArray, 3);
        }

        /*
        Example request paths applicable here:
        1) consents
        2) payments/{payment-product}
        3) consents/confirmation-of-funds
         */
        if (CommonConstants.FUNDS_CONFIRMATIONS_SERVICE_PATH.equals(pathElementAt(requestPathArray, 1))) {
            /*
            Example request paths applicable here:
            1) consents/confirmation-of-funds
             */
            return pathElementAt(requestPathArray, 1);
        }

        /*
        Example request paths applicable here:
        1) consents
        2) payments/{payment-product}
         */
        return pathElementAt(requestPathArray, 0);
    }

    /**
     * Reads an element of a request path that was split into its elements. A path shorter than the element
     * being read is reported as an empty path element, so that it is rejected as an unrecognised path by the
     * caller rather than failing the request with an index error.
     *
     * @param requestPathArray elements of the request path
     * @param index            index of the element to read
     * @return the path element, or an empty string if the path does not carry it
     */
    private static String pathElementAt(String[] requestPathArray, int index) {

        return index < requestPathArray.length ? requestPathArray[index] : "";
    }
}
