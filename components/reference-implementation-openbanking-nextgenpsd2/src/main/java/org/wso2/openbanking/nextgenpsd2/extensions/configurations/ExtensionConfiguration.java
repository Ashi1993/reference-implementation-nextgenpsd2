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

package org.wso2.openbanking.nextgenpsd2.extensions.configurations;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Configuration of the extension endpoints.
 * <p>
 * The configuration is read once, when the class is loaded, from the properties the web application ships
 * with. A deployment overrides a value without rebuilding the web application through a JVM system property,
 * through an environment variable carrying the name of the setting in upper case with dots replaced by
 * underscores, or through a properties file named by the {@value #CONFIG_FILE_PROPERTY} system property.
 * <p>
 * A value that cannot be read as the type it is used as is reported and the shipped default is used in its
 * place, so that one malformed setting does not take the extension endpoints down.
 */
public final class ExtensionConfiguration {

    /**
     * System property naming a properties file that overrides the shipped configuration.
     */
    public static final String CONFIG_FILE_PROPERTY = "nextgenpsd2.extension.config";

    private static final String DEFAULT_CONFIGURATION_RESOURCE = "nextgenpsd2-extension.properties";
    private static final String LIST_SEPARATOR = ",";

    private static final Log log = LogFactory.getLog(ExtensionConfiguration.class);

    private static final Properties DEFAULTS = readShippedConfiguration();
    private static final Properties CONFIGURATION = readDeploymentConfiguration();

    private ExtensionConfiguration() {
    }

    /**
     * Endpoint the metadata of the authorization server is read from.
     *
     * @return the OAuth metadata endpoint
     */
    public static String getOauthMetadataEndpoint() {
        return getString("oauth.metadata.endpoint");
    }

    /**
     * Endpoint the accounts a PSU may share are read from.
     *
     * @return the shareable accounts endpoint of the banking backend
     */
    public static String getShareableAccountsEndpoint() {
        return getString("backend.base.url") + getString("accounts.shareable.path");
    }

    /**
     * Endpoint the accounts a PSU may pay from are read from.
     *
     * @return the payable accounts endpoint of the banking backend
     */
    public static String getPayableAccountsEndpoint() {
        return getString("backend.base.url") + getString("accounts.payable.path");
    }

    /**
     * Endpoint a payment is submitted to or cancelled through.
     *
     * @return the payments endpoint of the banking backend
     */
    public static String getPaymentBackendUrl() {
        return getString("backend.base.url") + getString("payments.path");
    }

    /**
     * Whether Strong Customer Authentication is required for a consent.
     *
     * @return true if SCA is required
     */
    public static boolean isScaRequired() {
        return getBoolean("sca.required");
    }

    /**
     * The frequency per day an account consent may not go below.
     *
     * @return the configured minimum frequency per day
     */
    public static int getFrequencyPerDay() {
        return getInt("consent.frequency.per.day");
    }

    /**
     * Whether the validity an account consent may request is capped.
     *
     * @return true if the validity of a consent is capped
     */
    public static boolean isValidUntilDateCapEnabled() {
        return getBoolean("consent.valid.until.cap.enabled");
    }

    /**
     * The number of days an account consent may be valid for when the validity is capped.
     *
     * @return the number of days a consent may be valid for
     */
    public static int getValidUntilDays() {
        return getInt("consent.valid.until.days");
    }

    /**
     * The number of days into the future a payment may be scheduled for.
     *
     * @return the number of days a payment may be scheduled into the future
     */
    public static int getMaxFuturePaymentDays() {
        return getInt("payments.max.future.days");
    }

    /**
     * Whether a data access request is validated against the accounts bound to the consent.
     *
     * @return true if the requested account ID is validated
     */
    public static boolean isAccountIdValidationEnabled() {
        return getBoolean("consent.account.id.validation.enabled");
    }

    /**
     * Whether a funds confirmation request is validated against the account bound to the consent.
     *
     * @return true if the account of a funds confirmation request is validated
     */
    public static boolean isCofAccountIdValidationEnabled() {
        return getBoolean("consent.cof.account.id.validation.enabled");
    }

    /**
     * Whether a data access request is rejected when the accelerator sends no data request context.
     *
     * @return true if the data request context is required
     */
    public static boolean isConsentAccessContextRequired() {
        return getBoolean("consent.access.context.required");
    }

    /**
     * The account reference types the bank accepts, in the order they are read from an account reference.
     *
     * @return the supported account reference types
     */
    public static List<String> getSupportedAccountReferenceTypes() {
        return getList("accounts.supported.reference.types");
    }

    /**
     * Version of the account information service.
     *
     * @return the AIS API version
     */
    public static String getAisApiVersion() {
        return getString("api.version.ais");
    }

    /**
     * Version of the payment initiation service.
     *
     * @return the PIS API version
     */
    public static String getPisApiVersion() {
        return getString("api.version.pis");
    }

    /**
     * Version of the funds confirmation service.
     *
     * @return the PIIS API version
     */
    public static String getPiisApiVersion() {
        return getString("api.version.piis");
    }

    /**
     * The SCA approaches the deployment supports, named by their identifier.
     *
     * @return identifiers of the supported SCA approaches
     */
    public static List<String> getSupportedScaApproachIds() {
        return getList("sca.approaches");
    }

    /**
     * The SCA methods the deployment supports, named by their identifier.
     *
     * @return identifiers of the supported SCA methods
     */
    public static List<String> getSupportedScaMethodIds() {
        return getList("sca.methods");
    }

    /**
     * Reads a setting of a supported SCA approach.
     *
     * @param approachId identifier of the approach
     * @param setting    name of the setting, as it reads in sca.approach.&lt;approachId&gt;.&lt;setting&gt;
     * @return the configured value
     */
    public static String getScaApproachSetting(String approachId, String setting) {
        return getString(String.format("sca.approach.%s.%s", approachId, setting));
    }

    /**
     * Reads a setting of a supported SCA method.
     *
     * @param methodId identifier of the method
     * @param setting  name of the setting, as it reads in sca.method.&lt;methodId&gt;.&lt;setting&gt;
     * @return the configured value
     */
    public static String getScaMethodSetting(String methodId, String setting) {
        return getString(String.format("sca.method.%s.%s", methodId, setting));
    }

    /**
     * Reads a setting, preferring the value a system property or an environment variable carries over the
     * value the configuration of the deployment carries.
     *
     * @param key name of the setting
     * @return the configured value
     */
    private static String getString(String key) {
        String fromSystemProperty = System.getProperty(key);
        if (StringUtils.isNotBlank(fromSystemProperty)) {
            return fromSystemProperty.trim();
        }

        String fromEnvironment = System.getenv(key.replace('.', '_').toUpperCase(Locale.ROOT));
        if (StringUtils.isNotBlank(fromEnvironment)) {
            return fromEnvironment.trim();
        }

        String configured = CONFIGURATION.getProperty(key);
        return configured == null ? null : configured.trim();
    }

    /**
     * Reads a setting carrying a whole number, reporting a value that cannot be read as one and using the
     * shipped default in its place.
     *
     * @param key name of the setting
     * @return the configured value
     */
    private static int getInt(String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException e) {
            log.error(String.format("Configured value is not a number, using the default shipped for it. "
                    + "Setting: %s", key), e);
            return Integer.parseInt(DEFAULTS.getProperty(key).trim());
        }
    }

    /**
     * Reads a setting carrying a true or false value. A value that reads as neither is false, as
     * {@link Boolean#parseBoolean(String)} reports it.
     *
     * @param key name of the setting
     * @return the configured value
     */
    private static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    /**
     * Reads a setting carrying a comma separated list of values.
     *
     * @param key name of the setting
     * @return the configured values, which is empty when the setting carries none
     */
    private static List<String> getList(String key) {
        String configured = getString(key);

        if (StringUtils.isBlank(configured)) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (String value : configured.split(LIST_SEPARATOR)) {
            if (StringUtils.isNotBlank(value)) {
                values.add(value.trim());
            }
        }

        return Collections.unmodifiableList(values);
    }

    /**
     * Reads the configuration the web application ships with, which carries a value for every setting.
     *
     * @return the shipped configuration
     */
    private static Properties readShippedConfiguration() {
        Properties shipped = new Properties();

        try (InputStream configuration = ExtensionConfiguration.class.getClassLoader()
                .getResourceAsStream(DEFAULT_CONFIGURATION_RESOURCE)) {
            if (configuration == null) {
                throw new IllegalStateException("The extension configuration is missing from the web application");
            }
            shipped.load(configuration);
        } catch (IOException e) {
            throw new IllegalStateException("The extension configuration of the web application cannot be read", e);
        }

        return shipped;
    }

    /**
     * Reads the configuration of the deployment, which overrides the settings it carries and inherits the
     * rest from the configuration the web application ships with.
     *
     * @return the configuration of the deployment
     */
    private static Properties readDeploymentConfiguration() {
        Properties configuration = new Properties(DEFAULTS);
        String configurationFile = System.getProperty(CONFIG_FILE_PROPERTY);

        if (StringUtils.isBlank(configurationFile)) {
            return configuration;
        }

        try (InputStream overrides = Files.newInputStream(Paths.get(configurationFile.trim()))) {
            configuration.load(overrides);
            if (log.isDebugEnabled()) {
                log.debug("Read the extension configuration of the deployment");
            }
        } catch (IOException | RuntimeException e) {
            // The deployment keeps running on the shipped configuration rather than failing to serve at all
            log.error("The extension configuration of the deployment cannot be read, so the configuration "
                    + "shipped with the web application is used", e);
        }

        return configuration;
    }
}
