package dev.onebrain.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

public record ApiError(String code, String message, String requestId) {
    public static void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new JsonMapper().writeValue(response.getWriter(),
                new ApiError(code, message, response.getHeader("X-Request-Id")));
    }
}
