package com.link.up.server.http.servlet;

import com.link.up.server.http.FluxServlet;
import com.link.up.server.service.JobRestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 单节点离线 Worker 身份、容量和实例信息。
 */
public final class NodeServlet
        extends FluxServlet {

    private final JobRestService service;

    public NodeServlet(JobRestService service) {
        this.service = service;
    }

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        write(response, 200, service.node());
    }
}
