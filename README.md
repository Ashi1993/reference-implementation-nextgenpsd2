<!--
 ~ Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 ~
 ~ WSO2 LLC. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied. See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
-->

# WSO2 Open Banking NextGenPSD2 Reference Implementation

This project provides the reference implementation for the Berlin NextGenPSD2 specification, enabling banks and financial institutions to comply with PSD2 requirements.

## Overview

With the release of Open Banking 4.0.0, it has introduced OpenAPI based extensions such that the toolkit developer
can implement Open Banking specification requirements in their preferred programming language.
The custom developed extensions can be deployed externally and tested separately without restarting the WSO2 servers.

The OpenAPI extension can be found from [here](https://ob.docs.wso2.com/en/latest/references/accelerator-extensions-api/).

This reference implementation provides a comprehensive solution for implementing Open Banking NextGenPSD2 specifications, including:

- **Reference Implementation Components**: Core Berlin NextGenPSD2 implementation
- **Self-Care Portal**: React-based application for account consent management
- **Integration Test Suite**: Automated testing framework for Berlin toolkit validation
- **Open Banking Test Suite**: Comprehensive test framework for Berlin API compliance

## Prerequisites

- Java 11 or above
- [Apache Maven 3.0.5](https://maven.apache.org/download.cgi) or above
- MySQL 5.7 or above
- Node.js 18.16.0 and above and npm 9.7.0 and above (for building React applications)


## Project Structure

```
reference-implementation-nextgenpsd2/
├── components/                              # Core components
│   └── reference-implementation-openbanking-nextgenpsd2/
├── react-apps/                              # Frontend applications
│   └── self-care-portal/                   # User consent management portal
├── integration-test-suite/                  # Integration test framework
│   └── berlin-toolkit-integration-test/
├── open-banking-test-suite/                 # API compliance tests
│   └── toolkit-berlin-test/
```

## Building from Source

### Clone the Repository

```bash
git clone https://github.com/wso2/reference-implementation-nextgenpsd2.git
cd components/reference-implementation-openbanking-nextgenpsd2
```

### Build Commands

| Command | Description |
| :--- | :--- |
| `mvn install` | Build the project without cleaning the folders |
| `mvn clean install` | Clean and build from scratch |

## Installation and Setup

1. Goto /components/reference-implementation-openbanking-nextgenpsd2/target/ folder and host the `api#reference-implementation#ob#nextgenpsd2.war` in a preferred location and get the base URL.

NOTE:
- If you are hosting this in WSO2 Identity Server copy the `api#reference-implementation#ob#nextgenpsd2.war` to the `<IS_HOME>/repository/deployment/server/webapps` folder.
- Add the following configurations to the deployment.toml file inside the `<IS_HOME>/repository/conf` folder.
  ```
  [[resource.access_control]]
  context = "(.*)/api/reference-implementation/ob/nextgenpsd2/(.*)"
  http_method = "all"
  secure = "false"
  ```

## Configuring WSO2 Open Banking Accelerator 4.0.0 

1. Update the following configurations in the deployment.toml file inside the `<IS_HOME>/repository/conf` folder.

```
[financial_services.extensions.endpoint]
enabled = true
# allowed extensions: "pre_process_client_creation", "pre_process_consent_creation"
allowed_extensions = ["pre_process_client_creation", "pre_process_client_update", "pre_process_client_retrieval",
    "pre_process_consent_creation", "enrich_consent_creation_response", "pre_process_consent_file_upload",
    "enrich_consent_file_response", "pre_process_consent_retrieval", "validate_consent_file_retrieval",
    "pre_process_consent_revoke", "enrich_consent_search_response", "populate_consent_authorize_screen",
    "persist_authorized_consent", "validate_consent_access", "issue_refresh_token", "validate_authorization_request",
    "validate_event_subscription", "enrich_event_subscription_response", "validate_event_creation",
    "validate_event_polling", "enrich_event_polling_response", "map_accelerator_error_response"]
base_url = "https://<HOSTNAME>:<PORT>/api/reference-implementation/ob/nextgenpsd2/"
retry_count = 5
connect_timeout = 5
read_timeout = 5

[financial_services.extensions.endpoint.security]
# supported types : Basic-Auth or OAuth2
type = "Basic-Auth"
username = "<USERNAME>"
password = "<PASSWORD>"
```

2. Start the IS server.

## Configuring the Extension

The extension endpoints read their configuration from `nextgenpsd2-extension.properties`, which ships inside
the web application at `WEB-INF/classes/nextgenpsd2-extension.properties` and documents every setting it
carries. The defaults point at a locally hosted demo backend and authorization server.

A deployment overrides a setting without rebuilding the web application, in this order of precedence:

1. a JVM system property of the same name, for example `-Dsca.required=false`
2. an environment variable of the same name in upper case with dots replaced by underscores, for example
   `SCA_REQUIRED=false`
3. a properties file holding only the keys to override, named by the `nextgenpsd2.extension.config` system
   property:
   ```
   -Dnextgenpsd2.extension.config=<IS_HOME>/repository/conf/nextgenpsd2-extension.properties
   ```
4. the file shipped with the web application

The settings cover the banking backend and authorization server endpoints, the consent initiation limits, the
API versions of the nextGenPSD2 services, the supported SCA approaches and methods, and how strictly a data
access request is validated.

> **Note:** `consent.account.id.validation.enabled`, `consent.cof.account.id.validation.enabled` and
> `consent.access.context.required` are off by default, which is what a deployment against the declared
> extensions contract requires. Each of them lets through a data access request that a stricter deployment
> would reject, so turn them on once the accelerator of the deployment is known to send the data the
> validation needs.

## Running Tests

### Berlin Test Suite

1. Configure `test-config.xml` in `open-banking-test-suite/toolkit-berlin-test/resources/`
2. Navigate to the specific test package directory
3. Run tests with:
   ```bash
   mvn clean install -DgroupToRun=<apiVersion>
   # or
   mvn test -DgroupToRun=<apiVersion>
   ```

For detailed test execution instructions, refer to [toolkit-berlin-test README](open-banking-test-suite/toolkit-berlin-test/README.md).

## Self-Care Portal

The Self-Care Portal is a React-based application for managing account access consents. See [Self-Care Portal README](react-apps/self-care-portal/README.md) for development and deployment instructions.

## Reporting Issues

We encourage you to report issues, documentation faults, and feature requests through the [WSO2 Open Banking Berlin Toolkit Issue Tracker](https://github.com/wso2/reference-implementation-nextgenpsd2/issues).

## License

WSO2 Inc. licenses this source under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
