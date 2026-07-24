package com.link.up.server.http.servlet;

import com.link.up.server.http.FluxServlet;
import com.link.up.server.service.JobRestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * /api/v1/jobs 集合资源。
 */
public final class JobsServlet
        extends FluxServlet {

    private final JobRestService service;
    private final int maxRequestBytes;

    public JobsServlet(
            JobRestService service,
            int maxRequestBytes) {

        this.service = service;
        this.maxRequestBytes = maxRequestBytes;
    }

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        validateSubmitContentType(request);

        write(
                response,
                202,
                service.submit(
                        requestBody(
                                request,
                                maxRequestBytes)));
    }


    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        int page =
                intParameter(
                        request,
                        "page",
                        1);

        int pageSize =
                intParameter(
                        request,
                        "pageSize",
                        20);

        write(
                response,
                200,
                service.jobs(
                        request.getParameter("status"),
                        page,
                        pageSize));
    }
}
