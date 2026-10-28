package org.wso2.openbanking.nextgenpsd2.extensions.api;

import io.swagger.annotations.*;
import org.wso2.openbanking.nextgenpsd2.extensions.model.error.ErrorResponse;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.AppCreateProcessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.Response200ForApplicationCreation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.SuccessResponseApplicationCreation;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.SuccessResponseApplicationCreationData;
import org.wso2.openbanking.nextgenpsd2.extensions.impl.application.PreProcessApplicationCreationApiImpl;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
* Represents a collection of functions to interact with the API endpoints.
*/
@Path("/pre-process-application-creation")
@Api(description = "the pre-process-application-creation API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2025-06-02T10:40:43.095685+05:30[Asia/Colombo]", comments = "Generator version: 7.13.0")
public class PreProcessApplicationCreationApi {

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "handle pre validations & changes to the consumer application creation", notes = "", response = Response200ForApplicationCreation.class, authorizations = {
        @Authorization(value = "OAuth2", scopes = {
             }),
        
        @Authorization(value = "BasicAuth")
         }, tags={ "Application" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Ok", response = Response200ForApplicationCreation.class),
        @ApiResponse(code = 400, message = "Bad Request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Server Error", response = ErrorResponse.class)
    })
    public Response preProcessApplicationCreationPost(@Valid @NotNull AppCreateProcessRequestBody appCreateProcessRequestBody) {

        return PreProcessApplicationCreationApiImpl.handleMCRApplicationCreation(appCreateProcessRequestBody);
    }
}
