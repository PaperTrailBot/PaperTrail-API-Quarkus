package io.github.eggy03.papertrail.api.exceptions.mapper;

import io.github.eggy03.papertrail.api.annotation.UTCClock;
import io.github.eggy03.papertrail.api.exceptions.entity.ErrorResponse;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jspecify.annotations.NonNull;

import java.time.Clock;

@Provider
public class ResteasyReactiveViolationExceptionMapper implements ExceptionMapper<ResteasyReactiveViolationException> {

    private final @NonNull Clock clock;
    @Context
    UriInfo uriInfo;

    public ResteasyReactiveViolationExceptionMapper(@NonNull @UTCClock Clock clock) {
        this.clock = clock;
    }

    @Override
    public Response toResponse(ResteasyReactiveViolationException e) {

        ErrorResponse errorResponse = new ErrorResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                e.getClass().getSimpleName(),
                e.getMessage() == null ? "N/A" : e.getMessage(),
                clock.instant(),
                uriInfo.getPath() == null ? "N/A" : uriInfo.getPath()
        );

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }

}
