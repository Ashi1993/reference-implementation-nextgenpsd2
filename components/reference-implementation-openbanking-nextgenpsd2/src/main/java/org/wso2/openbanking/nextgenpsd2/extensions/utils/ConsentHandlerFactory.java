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

import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.AuthorizationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ValidationFailureException;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentAuthorizationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.ConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.AccountConsentAuthorizeHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.AccountConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.AccountConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.ConsentAuthorisationInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.FundsConfirmationConsentAuthorizeHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.FundsConfirmationConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.FundsConfirmationConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.PaymentConsentAuthorizeHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.PaymentConsentInitiationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.handlers.impl.PaymentConsentValidationHandler;
import org.wso2.openbanking.nextgenpsd2.extensions.model.TPPMessage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

/**
 * Factory of the handlers that carry the consent type specific behaviour of the extension endpoints.
 * <p>
 * The handlers hold no state of their own, so one instance of each is built when the factory is loaded and
 * shared by every request it serves.
 */
public final class ConsentHandlerFactory {

    private static final Map<String, ConsentInitiationHandler> INITIATION_HANDLERS = initiationHandlers();
    private static final Map<ExtensionEnums.ConsentTypeEnum, ConsentAuthorizationHandler> AUTHORIZATION_HANDLERS =
            authorizationHandlers();
    private static final Map<ExtensionEnums.ConsentTypeEnum, ConsentValidationHandler> VALIDATION_HANDLERS =
            validationHandlers();

    private ConsentHandlerFactory() {
    }

    /**
     * The initiation handler of every service a consent can be initiated under, keyed by the element of the
     * request path that identifies the service.
     *
     * @return the initiation handler of every supported service
     */
    private static Map<String, ConsentInitiationHandler> initiationHandlers() {
        ConsentInitiationHandler paymentHandler = new PaymentConsentInitiationHandler();
        ConsentInitiationHandler authorisationHandler = new ConsentAuthorisationInitiationHandler();

        Map<String, ConsentInitiationHandler> handlers = new HashMap<>();
        handlers.put(CommonConstants.ACCOUNTS_CONSENT_PATH, new AccountConsentInitiationHandler());
        handlers.put(CommonConstants.PAYMENTS_SERVICE_PATH, paymentHandler);
        handlers.put(CommonConstants.BULK_PAYMENTS_SERVICE_PATH, paymentHandler);
        handlers.put(CommonConstants.PERIODIC_PAYMENTS_SERVICE_PATH, paymentHandler);
        handlers.put(CommonConstants.FUNDS_CONFIRMATIONS_SERVICE_PATH,
                new FundsConfirmationConsentInitiationHandler());
        handlers.put(CommonConstants.EXPLICIT_AUTHORISATION_PATH_END, authorisationHandler);
        handlers.put(CommonConstants.PAYMENT_EXPLICIT_CANCELLATION_AUTHORISATION_PATH_END, authorisationHandler);

        return Collections.unmodifiableMap(handlers);
    }

    /**
     * The authorization handler of every consent type, keyed by the type.
     *
     * @return the authorization handler of every supported consent type
     */
    private static Map<ExtensionEnums.ConsentTypeEnum, ConsentAuthorizationHandler> authorizationHandlers() {
        ConsentAuthorizationHandler paymentHandler = new PaymentConsentAuthorizeHandler();

        Map<ExtensionEnums.ConsentTypeEnum, ConsentAuthorizationHandler> handlers =
                new EnumMap<>(ExtensionEnums.ConsentTypeEnum.class);
        handlers.put(ExtensionEnums.ConsentTypeEnum.ACCOUNTS, new AccountConsentAuthorizeHandler());
        handlers.put(ExtensionEnums.ConsentTypeEnum.PAYMENTS, paymentHandler);
        handlers.put(ExtensionEnums.ConsentTypeEnum.BULK_PAYMENTS, paymentHandler);
        handlers.put(ExtensionEnums.ConsentTypeEnum.PERIODIC_PAYMENTS, paymentHandler);
        handlers.put(ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION,
                new FundsConfirmationConsentAuthorizeHandler());

        return Collections.unmodifiableMap(handlers);
    }

    /**
     * The validation handler of every consent type, keyed by the type.
     *
     * @return the validation handler of every supported consent type
     */
    private static Map<ExtensionEnums.ConsentTypeEnum, ConsentValidationHandler> validationHandlers() {
        ConsentValidationHandler paymentHandler = new PaymentConsentValidationHandler();

        Map<ExtensionEnums.ConsentTypeEnum, ConsentValidationHandler> handlers =
                new EnumMap<>(ExtensionEnums.ConsentTypeEnum.class);
        handlers.put(ExtensionEnums.ConsentTypeEnum.ACCOUNTS, new AccountConsentValidationHandler());
        handlers.put(ExtensionEnums.ConsentTypeEnum.PAYMENTS, paymentHandler);
        handlers.put(ExtensionEnums.ConsentTypeEnum.BULK_PAYMENTS, paymentHandler);
        handlers.put(ExtensionEnums.ConsentTypeEnum.PERIODIC_PAYMENTS, paymentHandler);
        handlers.put(ExtensionEnums.ConsentTypeEnum.FUNDS_CONFIRMATION,
                new FundsConfirmationConsentValidationHandler());

        return Collections.unmodifiableMap(handlers);
    }

    /**
     * Returns the consent type a handler is keyed by, or null if the type is not one the solution supports.
     *
     * @param consentType consent type of the request
     * @return the consent type, or null if it is not supported
     */
    private static ExtensionEnums.ConsentTypeEnum toConsentType(String consentType) {
        try {
            return ExtensionEnums.ConsentTypeEnum.fromValue(consentType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Method to get the Consent Manage Service Handler.
     *
     * @param requestPath Request path of the request
     * @return the initiation handler of the service the request was made under
     * @throws ValidationFailureException if the request path does not belong to a supported service
     * @throws ExtensionException if construction of the error failed
     */
    public static ConsentInitiationHandler getConsentInitiationHandler(String requestPath)
            throws ExtensionException, ValidationFailureException {

        ConsentInitiationHandler handler = INITIATION_HANDLERS
                .get(ConsentPathUtil.getServiceDifferentiatingRequestPath(requestPath));

        if (handler == null) {
            throw new ValidationFailureException(Response.Status.NOT_FOUND,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR, null,
                            ErrorConstants.PATH_INVALID));
        }

        return handler;
    }

    /**
     * Method to get the authorization handler of the consent being authorized.
     *
     * @param consentType consent type of the consent being authorized
     * @return the selected authorization handler
     * @throws AuthorizationFailureException if the consent type is not one the solution supports
     */
    public static ConsentAuthorizationHandler getAuthorizationHandler(String consentType)
            throws AuthorizationFailureException {

        ConsentAuthorizationHandler handler = AUTHORIZATION_HANDLERS.get(toConsentType(consentType));

        if (handler == null) {
            // Should be unreachable since only consent types in ConsentTypes enum are used at initiation
            throw new AuthorizationFailureException(ErrorConstants.INITIATED_CONSENT_TYPE_INVALID);
        }

        return handler;
    }

    /**
     * Method to get the consent validation handler for the type of the consent being accessed.
     *
     * @param consentType consent type of the consent being accessed
     * @return the selected consent validation handler
     * @throws ValidationFailureException if the consent type is not recognised
     * @throws ExtensionException if construction of the error failed
     */
    public static ConsentValidationHandler getConsentValidationHandler(String consentType)
            throws ValidationFailureException, ExtensionException {

        ConsentValidationHandler handler = VALIDATION_HANDLERS.get(toConsentType(consentType));

        if (handler == null) {
            throw new ValidationFailureException(Response.Status.BAD_REQUEST,
                    ErrorUtil.constructBerlinError(null, TPPMessage.CategoryEnum.ERROR,
                            TPPMessage.CodeEnum.CONSENT_UNKNOWN, ErrorConstants.INITIATED_CONSENT_TYPE_INVALID));
        }

        return handler;
    }
}
