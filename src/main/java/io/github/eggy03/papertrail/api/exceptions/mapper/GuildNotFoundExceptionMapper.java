package io.github.eggy03.papertrail.api.exceptions.mapper;

import io.github.eggy03.papertrail.api.annotation.UTCClock;
import io.github.eggy03.papertrail.api.exceptions.GuildNotFoundException;
import io.github.eggy03.papertrail.api.exceptions.entity.ErrorResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jspecify.annotations.NonNull;

import java.time.Clock;

@Provider
public class GuildNotFoundExceptionMapper implements ExceptionMapper<GuildNotFoundException> {

    private final @NonNull Clock clock;
    @Context
    UriInfo uriInfo;

    public GuildNotFoundExceptionMapper(@NonNull @UTCClock Clock clock) {
        this.clock = clock;
    }

    @Override
    public Response toResponse(GuildNotFoundException e) {

        ErrorResponse errorResponse = new ErrorResponse(
                Response.Status.NOT_FOUND.getStatusCode(),
                e.getClass().getSimpleName(),
                e.getMessage() == null ? "N/A" : e.getMessage(),
                clock.instant(),
                uriInfo.getPath() == null ? "N/A" : uriInfo.getPath()
        );

        return Response
                .status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .build();
    }
}
