package io.github.eggy03.papertrail.api.exceptions.mapper;

import io.github.eggy03.papertrail.api.annotation.UTCClock;
import io.github.eggy03.papertrail.api.exceptions.entity.ErrorResponse;
import io.quarkus.arc.ArcUndeclaredThrowableException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;

import java.time.Clock;

@Provider
@Slf4j
public class ArcUndeclaredThrowableExceptionMapper implements ExceptionMapper<ArcUndeclaredThrowableException> {

    private final @NonNull Clock clock;
    @Context
    UriInfo uriInfo;

    public ArcUndeclaredThrowableExceptionMapper(@NonNull @UTCClock Clock clock) {
        this.clock = clock;
    }

    @Override
    public Response toResponse(ArcUndeclaredThrowableException e) {

        // CVE is wrapped in RollbackException which is wrapped in ArcUndeclaredThrowableException
        if (e.getCause().getCause() instanceof ConstraintViolationException cve) {
            return cveResponse(cve);
        }

        return genericResponse(e);
    }

    private Response cveResponse(ConstraintViolationException cve) {
        ErrorResponse errorResponse = new ErrorResponse(
                Response.Status.CONFLICT.getStatusCode(),
                cve.getClass().getSimpleName(),
                cve.getMessage() == null ? "N/A" : cve.getMessage(),
                clock.instant(),
                uriInfo.getPath() == null ? "N/A" : uriInfo.getPath()
        );

        return Response
                .status(Response.Status.CONFLICT)
                .entity(errorResponse)
                .build();
    }

    private Response genericResponse(ArcUndeclaredThrowableException e) {

        log.warn("Exception from a generated arc class", e);

        ErrorResponse errorResponse = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                e.getClass().getSimpleName(),
                e.getMessage() == null ? "N/A" : e.getMessage(),
                clock.instant(),
                uriInfo.getPath() == null ? "N/A" : uriInfo.getPath()
        );

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}
