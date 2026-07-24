package com.link.up.server.http.servlet;

import com.link.up.server.http.FluxServlet;
import com.link.up.server.runtime.JobNotFoundException;
import com.link.up.server.service.JobRestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 处理以下资源：
 *
 * <pre>
 * GET    /api/v1/jobs/{jobId}
 * DELETE /api/v1/jobs/{jobId}
 * GET    /api/v1/jobs/{jobId}/pipelines
 * GET    /api/v1/jobs/{jobId}/tasks
 * GET    /api/v1/jobs/{jobId}/metrics
 * </pre>
 */
public final class JobResourceServlet
        extends FluxServlet {

    private final JobRestService service;

    public JobResourceServlet(
            JobRestService service) {

        this.service = service;
    }

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        List<String> segments =
                pathSegments(request);

        if (segments.size() == 1) {
            write(
                    response,
                    200,
                    service.job(segments.get(0)));
            return;
        }

        if (segments.size() == 2) {
            String jobId =
                    segments.get(0);

            String resource =
                    segments.get(1);

            if ("pipelines".equals(resource)) {
                write(
                        response,
                        200,
                        service.pipelines(jobId));
                return;
            }

            if ("tasks".equals(resource)) {
                write(
                        response,
                        200,
                        service.tasks(jobId));
                return;
            }

            if ("metrics".equals(resource)) {
                write(
                        response,
                        200,
                        service.metrics(jobId));
                return;
            }
        }

        throw new JobNotFoundException(
                request.getRequestURI());
    }

    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        List<String> segments =
                pathSegments(request);

        if (segments.size() != 1) {
            throw new JobNotFoundException(
                    request.getRequestURI());
        }

        write(
                response,
                202,
                service.cancel(segments.get(0)));
    }
}
