package org.wso2.openbanking.nextgenpsd2.extensions.api;

import io.swagger.annotations.*;
import org.wso2.openbanking.nextgenpsd2.extensions.model.error.ErrorResponse;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.AppUpdateProcessRequestBody;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.Response200ForApplicationUpdate;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.SuccessResponseApplicationUpdate;
import org.wso2.openbanking.nextgenpsd2.extensions.model.application.SuccessResponseApplicationUpdateData;
import org.wso2.openbanking.nextgenpsd2.extensions.impl.application.PreProcessApplicationUpdateApiImpl;

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
@Path("/pre-process-application-update")
@Api(description = "the pre-process-application-update API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2025-06-02T10:40:43.095685+05:30[Asia/Colombo]", comments = "Generator version: 7.13.0")
public class PreProcessApplicationUpdateApi {

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "handle pre validations & changes to the consumer application update", notes = "", response = Response200ForApplicationUpdate.class, authorizations = {
        @Authorization(value = "OAuth2", scopes = {
             }),
        
        @Authorization(value = "BasicAuth")
         }, tags={ "Application" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Ok", response = Response200ForApplicationUpdate.class),
        @ApiResponse(code = 400, message = "Bad Request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Server Error", response = ErrorResponse.class)
    })
    public Response preProcessApplicationUpdatePost(@Valid @NotNull AppUpdateProcessRequestBody appUpdateProcessRequestBody) {

        return PreProcessApplicationUpdateApiImpl.handleMCRApplicationUpdate(appUpdateProcessRequestBody);
    }
}
