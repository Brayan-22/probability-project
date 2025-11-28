package com.probabilidad.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        String errorId = UUID.randomUUID().toString();

        // Log completo del error con stack trace
        LOG.errorf(exception, "Error no controlado [ID: %s]: %s", errorId, exception.getMessage());

        // Log adicional con información detallada
        LOG.errorf("Error ID: %s", errorId);
        LOG.errorf("Exception type: %s", exception.getClass().getName());
        LOG.errorf("Message: %s", exception.getMessage());

        if (exception.getCause() != null) {
            LOG.errorf("Caused by: %s - %s",
                exception.getCause().getClass().getName(),
                exception.getCause().getMessage());
        }

        // Respuesta al cliente
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("details", "Error id " + errorId);
        errorResponse.put("stack", ""); // No exponemos stack trace al cliente en producción

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}
