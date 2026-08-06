package com.link.up.server.http.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.link.up.api.job.JobSpec;
import com.link.up.server.dto.JobSubmitRequest;
import com.link.up.server.http.FluxServlet;
import com.link.up.server.http.JsonSupport;
import com.link.up.server.service.JobRestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * /api/v1/jobs 集合资源。
 */
public final class JobsServlet
        extends FluxServlet {

    private static final Logger LOG =
            LogManager.getLogger(
                    JobsServlet.class);

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

        String body =
                requestBody(
                        request,
                        maxRequestBytes);

        Object result;

        if (isJson(request)) {
            try {
                JobSubmitRequest submitRequest =
                        JsonSupport.mapper()
                                .readValue(
                                        body,
                                        JobSubmitRequest.class);

                logSubmissionSummary(
                        request,
                        submitRequest,
                        body);

                result = service.submit(submitRequest);
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException(
                        "Invalid JSON submit request");
            }
        } else {
            LOG.info(
                    "Received legacy job submission, contentType={}, bodyBytes={}",
                    request.getContentType(),
                    utf8Length(body));

            result = service.submitLegacyResponse(body);
        }

        write(response, 202, result);
    }

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        String externalExecutionId =
                request.getParameter("externalExecutionId");

        if (externalExecutionId != null
                && !externalExecutionId.trim().isEmpty()) {

            write(
                    response,
                    200,
                    service.jobByExternalExecutionId(
                            externalExecutionId));
            return;
        }

        int page = intParameter(request, "page", 1);
        int pageSize = intParameter(request, "pageSize", 20);

        write(
                response,
                200,
                service.executionPage(
                        request.getParameter("status"),
                        page,
                        pageSize));
    }

    private void logSubmissionSummary(
            HttpServletRequest request,
            JobSubmitRequest submitRequest,
            String body) {

        JobSpec jobSpec =
                submitRequest == null
                        ? null
                        : submitRequest.getJobSpec();

        LOG.info(
                "Received job submission, contentType={}, bodyBytes={}, externalExecutionId={}, definitionVersion={}, jobName={}, sourceConnector={}, sinkConnector={}",
                request.getContentType(),
                utf8Length(body),
                submitRequest == null
                        ? null
                        : submitRequest.getExternalExecutionId(),
                submitRequest == null
                        ? null
                        : submitRequest.getDefinitionVersion(),
                jobSpec == null
                        ? null
                        : jobSpec.getName(),
                connectorId(
                        jobSpec == null
                                ? null
                                : jobSpec.getSource()),
                connectorId(
                        jobSpec == null
                                ? null
                                : jobSpec.getSink()));
    }

    private String connectorId(
            JobSpec.Connector connector) {

        return connector == null
                ? null
                : connector.getConnectorId();
    }

    private int utf8Length(String value) {
        return value == null
                ? 0
                : value.getBytes(
                        StandardCharsets.UTF_8)
                        .length;
    }

    private boolean isJson(HttpServletRequest request) {
        String contentType = request.getContentType();

        if (contentType == null) {
            return false;
        }

        int separator = contentType.indexOf(';');
        String mediaType =
                separator >= 0
                        ? contentType.substring(0, separator)
                        : contentType;

        return "application/json".equals(
                mediaType.trim()
                        .toLowerCase(Locale.ROOT));
    }
}
