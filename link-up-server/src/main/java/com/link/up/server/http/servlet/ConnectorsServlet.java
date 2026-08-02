package com.link.up.server.http.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.link.up.server.http.FluxServlet;
import com.link.up.server.http.JsonSupport;
import com.link.up.server.http.RestException;
import com.link.up.server.service.ConnectorPreflightRequest;
import com.link.up.server.service.ConnectorRestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Connector Schema 查询与只读预检接口。
 */
public final class ConnectorsServlet
        extends FluxServlet {

    private static final int MAX_PREFLIGHT_REQUEST_BYTES =
            1024 * 1024;

    private final ConnectorRestService service;

    public ConnectorsServlet(
            ConnectorRestService service) {

        this.service = service;
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        List<String> segments =
                pathSegments(request);

        if (segments.isEmpty()) {
            write(
                    response,
                    200,
                    service.list(
                            request.getParameter(
                                    "role")));
            return;
        }

        if (segments.size() == 2
                && "schema".equalsIgnoreCase(
                segments.get(1))) {

            write(
                    response,
                    200,
                    service.schema(
                            segments.get(0),
                            request.getParameter(
                                    "role")));
            return;
        }

        throw notFound();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        List<String> segments =
                pathSegments(request);

        if (segments.size() == 2
                && "preflight".equalsIgnoreCase(
                segments.get(1))) {

            validateSubmitContentType(
                    request);

            ConnectorPreflightRequest body;

            try {
                String json =
                        requestBody(
                                request,
                                MAX_PREFLIGHT_REQUEST_BYTES);

                body =
                        json == null
                                || json.trim().isEmpty()
                                ? new ConnectorPreflightRequest()
                                : JsonSupport.mapper()
                                .readValue(
                                        json,
                                        ConnectorPreflightRequest.class);
            } catch (JsonProcessingException exception) {
                throw new RestException(
                        400,
                        "FLUX-CONNECTOR-PREFLIGHT-JSON-INVALID",
                        "Connector preflight request must be valid JSON");
            }

            write(
                    response,
                    200,
                    service.preflight(
                            segments.get(0),
                            request.getParameter(
                                    "role"),
                            body.getOptions()));
            return;
        }

        throw notFound();
    }

    private RestException notFound() {
        return new RestException(
                404,
                "FLUX-REST-404",
                "Connector resource not found");
    }
}
