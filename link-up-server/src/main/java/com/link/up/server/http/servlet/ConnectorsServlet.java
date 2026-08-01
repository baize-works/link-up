package com.link.up.server.http.servlet;

import com.link.up.server.http.FluxServlet;
import com.link.up.server.http.RestException;
import com.link.up.server.service.ConnectorRestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Connector Schema 只读接口。
 */
public final class ConnectorsServlet
        extends FluxServlet {

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

        throw new RestException(
                404,
                "FLUX-REST-404",
                "Connector resource not found");
    }
}
