package com.link.up.server.http.servlet;

import com.link.up.server.http.FluxServlet;
import com.link.up.server.http.RestException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class NotFoundServlet
        extends FluxServlet {

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) {

        throw notFound(request);
    }

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) {

        throw notFound(request);
    }

    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response) {

        throw notFound(request);
    }

    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response) {

        throw notFound(request);
    }

    private RestException notFound(
            HttpServletRequest request) {

        return new RestException(
                404,
                "FLUX-REST-404",
                "Resource not found: "
                        + request.getRequestURI());
    }
}