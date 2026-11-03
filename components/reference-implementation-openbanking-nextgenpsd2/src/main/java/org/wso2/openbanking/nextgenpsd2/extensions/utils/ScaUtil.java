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

import org.json.JSONObject;
import org.wso2.openbanking.nextgenpsd2.extensions.configurations.ExtensionConfiguration;
import org.wso2.openbanking.nextgenpsd2.extensions.exceptions.ExtensionException;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ScaApproach;
import org.wso2.openbanking.nextgenpsd2.extensions.model.ScaMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

/**
 * Utility class for resolving the Strong Customer Authentication approach and methods of a consent.
 * <p>
 * The supported approaches and methods are read from the configuration once, since they are fixed for the
 * lifetime of the deployment.
 */
public final class ScaUtil {

    private static final String TYPE_SETTING = "type";
    private static final String VERSION_SETTING = "version";
    private static final String NAME_SETTING = "name";
    private static final String MAPPED_APPROACH_SETTING = "mapped.approach";
    private static final String DESCRIPTION_SETTING = "description";
    private static final String DEFAULT_SETTING = "default";

    private static final List<ScaApproach> SUPPORTED_SCA_APPROACHES = Collections.unmodifiableList(
            readSupportedScaApproaches());
    private static final List<ScaMethod> SUPPORTED_SCA_METHODS = Collections.unmodifiableList(
            readSupportedScaMethods());

    private ScaUtil() {
    }

    /**
     * Returns authorisation status based on preference.
     * @param isSCARequired whether Strong Customer Authentication is required for consents
     * @param headers request headers
     * @return the authorization status after consent creation (implicit authorization)
     */
    public static String getAuthorizationStatus(boolean isSCARequired, JSONObject headers) {
        return (isSCARequired) ?
                ExtensionEnums.ScaStatusEnum.SCA_METHOD_SELECTED.toString() :
                headers.has(CommonConstants.PSU_ID_HEADER) ?
                        ExtensionEnums.ScaStatusEnum.PSU_IDENTIFIED.toString() :
                        ExtensionEnums.ScaStatusEnum.RECEIVED.toString();
    }

    /**
     * Gets the configured default SCA approach.
     *
     * @return default SCA approach
     */
    public static Optional<ScaApproach> getDefaultScaApproach() {

        return getSupportedScaApproaches().stream().filter(ScaApproach::isDefault).findFirst();
    }

    /**
     * Gets the configured default SCA method.
     *
     * @return default SCA method
     */
    public static Optional<ScaMethod> getDefaultScaMethod() {

        return getSupportedScaMethods().stream().filter(ScaMethod::isDefault).findFirst();
    }

    /**
     * Gets the SCA approach from the list of approaches.
     *
     * @param scaApproachEnum the SCA approach to find
     * @return found SCA approach
     */
    public static Optional<ScaApproach> getScaApproach(ExtensionEnums.ScaApproachEnum scaApproachEnum) {

        return getSupportedScaApproaches().stream()
                .filter(scaApproach -> scaApproachEnum.equals(scaApproach.getApproach()))
                .findFirst();
    }

    /**
     * Determines the current SCA approach and the SCA methods based on the parameters.
     *
     * @param isTppRedirectPreferred TPP-Redirect-Preferred header
     * @param isScaRequired          to know if SCA is required or not
     * @return returns the current SCA approach and methods
     */
    public static Map<String, Object> getScaApproachAndMethods(Boolean isTppRedirectPreferred, boolean isScaRequired)
            throws ExtensionException {
        List<ScaMethod> supportedScaMethods = getSupportedScaMethods();

        Optional<ScaApproach> currentScaApproach = Optional.empty();
        List<ScaMethod> currentScaMethods = new ArrayList<>();

        if (Boolean.TRUE.equals(isTppRedirectPreferred)) {
            currentScaApproach = getScaApproach(ExtensionEnums.ScaApproachEnum.REDIRECT);
            addScaMethod(currentScaMethods, ExtensionEnums.ScaApproachEnum.REDIRECT, isScaRequired);
        } else if (Boolean.FALSE.equals(isTppRedirectPreferred)) {
            currentScaApproach = getScaApproach(ExtensionEnums.ScaApproachEnum.DECOUPLED);
            addScaMethod(currentScaMethods, ExtensionEnums.ScaApproachEnum.DECOUPLED, isScaRequired);
        } else {
            // When TPP-Redirect-Preferred header is not sent
            if (isScaRequired) {
                if (supportedScaMethods.size() == 1) {
                    // If SCA is required and there is only a single supported SCA method, it becomes the selected
                    // SCA method and the mapped approach of the SCA method becomes the SCA approach
                    currentScaApproach = getScaApproach(supportedScaMethods.get(0).getMappedApproach());
                    currentScaMethods = new ArrayList<>(supportedScaMethods);
                } else {
                    // If a default SCA method is configured then that becomes the SCA approach and method
                    Optional<ScaMethod> defaultScaMethod = getDefaultScaMethod();

                    if (defaultScaMethod.isPresent()) {
                        currentScaApproach = getDefaultScaApproach();
                        currentScaMethods.add(defaultScaMethod.get());
                    } else {
                        // Approach not finalised and the TPP/PSU will be given the choice to select
                        currentScaMethods = new ArrayList<>(supportedScaMethods);
                    }
                }
            } else {
                currentScaApproach = getDefaultScaApproach();
            }
        }

        if (!currentScaApproach.isPresent()) {
            // A deployment that supports no approach for the request cannot carry it out
            throw new ExtensionException(Response.Status.INTERNAL_SERVER_ERROR, ErrorConstants.SERVER_ERROR,
                    ErrorConstants.SCA_APPROACH_NOT_CONFIGURED);
        }

        Map<String, Object> scaApproachAndMethods = new HashMap<>();
        scaApproachAndMethods.put(CommonConstants.SCA_APPROACH_KEY, currentScaApproach.get());
        scaApproachAndMethods.put(CommonConstants.SCA_METHODS_KEY, currentScaMethods);

        return scaApproachAndMethods;
    }

    /**
     * Adds the SCA method of an approach to the methods of the request, when the request requires SCA and the
     * deployment supports a method for the approach.
     *
     * @param scaMethods      SCA methods of the request
     * @param scaApproachEnum approach the method is added for
     * @param isScaRequired   whether the request requires SCA
     */
    private static void addScaMethod(List<ScaMethod> scaMethods, ExtensionEnums.ScaApproachEnum scaApproachEnum,
                                     boolean isScaRequired) {
        if (isScaRequired) {
            getScaMethod(scaApproachEnum).ifPresent(scaMethods::add);
        }
    }

    /**
     * Gets the SCA method for the specified approach from the list of methods.
     *
     * @param scaApproachEnum the SCA method to find
     * @return found SCA method
     */
    public static Optional<ScaMethod> getScaMethod(ExtensionEnums.ScaApproachEnum scaApproachEnum) {

        return getSupportedScaMethods().stream()
                .filter(scaMethod -> scaApproachEnum.equals(scaMethod.getMappedApproach()))
                .findFirst();
    }

    /**
     * The SCA methods the deployment supports.
     *
     * @return the configured SCA methods
     */
    public static List<ScaMethod> getSupportedScaMethods() {
        return SUPPORTED_SCA_METHODS;
    }

    /**
     * The SCA approaches the deployment supports.
     *
     * @return the configured SCA approaches
     */
    public static List<ScaApproach> getSupportedScaApproaches() {
        return SUPPORTED_SCA_APPROACHES;
    }

    /**
     * Reads the configured SCA methods, mapping each to the ScaMethod class.
     *
     * @return the configured SCA methods
     */
    private static List<ScaMethod> readSupportedScaMethods() {
        List<ScaMethod> supportedScaMethods = new ArrayList<>();

        for (String methodId : ExtensionConfiguration.getSupportedScaMethodIds()) {
            ScaMethod scaMethod = new ScaMethod();

            scaMethod.setAuthenticationMethodId(methodId);
            scaMethod.setAuthenticationType(ExtensionConfiguration.getScaMethodSetting(methodId, TYPE_SETTING));
            scaMethod.setAuthenticationVersion(ExtensionConfiguration.getScaMethodSetting(methodId, VERSION_SETTING));
            scaMethod.setName(ExtensionConfiguration.getScaMethodSetting(methodId, NAME_SETTING));
            scaMethod.setMappedApproach(ExtensionEnums.ScaApproachEnum.fromValue(
                    ExtensionConfiguration.getScaMethodSetting(methodId, MAPPED_APPROACH_SETTING)));
            scaMethod.setDescription(ExtensionConfiguration.getScaMethodSetting(methodId, DESCRIPTION_SETTING));
            scaMethod.setDefault(Boolean.parseBoolean(
                    ExtensionConfiguration.getScaMethodSetting(methodId, DEFAULT_SETTING)));

            supportedScaMethods.add(scaMethod);
        }

        return supportedScaMethods;
    }

    /**
     * Reads the configured SCA approaches, mapping each to the ScaApproach class.
     *
     * @return the configured SCA approaches
     */
    private static List<ScaApproach> readSupportedScaApproaches() {
        List<ScaApproach> supportedScaApproaches = new ArrayList<>();

        for (String approachId : ExtensionConfiguration.getSupportedScaApproachIds()) {
            ScaApproach scaApproach = new ScaApproach();

            scaApproach.setApproach(ExtensionEnums.ScaApproachEnum.fromValue(approachId));
            scaApproach.setDefault(Boolean.parseBoolean(
                    ExtensionConfiguration.getScaApproachSetting(approachId, DEFAULT_SETTING)));

            supportedScaApproaches.add(scaApproach);
        }

        return supportedScaApproaches;
    }
}
