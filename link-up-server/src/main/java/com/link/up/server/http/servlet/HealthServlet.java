package com.link.up.server.http.servlet;

import com.link.up.server.http.FluxServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HealthServlet
        extends FluxServlet {

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        Map<String, Object> result =
                new LinkedHashMap<String, Object>();

        result.put("status", "UP");
        result.put(
                "service",
                "link-up-server");
        result.put(
                "timestamp",
                System.currentTimeMillis());

        write(
                response,
                200,
                result);
    }
}
