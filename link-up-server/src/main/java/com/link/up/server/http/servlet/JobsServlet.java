package com.link.up.server.http.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.link.up.server.dto.JobSubmitRequest;
import com.link.up.server.http.FluxServlet;
import com.link.up.server.http.JsonSupport;
import com.link.up.server.service.JobRestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

// 在类中定义

/**
 * /api/v1/jobs 集合资源。
 */
public final class JobsServlet
        extends FluxServlet {
    private static final Logger LOG =
            LogManager.getLogger(JobsServlet.class);
    private static final ObjectMapper FORMATTED_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

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
        LOG.info(
                "Received job submission, contentType={}, body={}",
                request.getContentType(),
                FORMATTED_MAPPER.writeValueAsString(
                        FORMATTED_MAPPER.readValue(body, Object.class)
                )
        );
        Object result;

        if (isJson(request)) {
            try {
                JobSubmitRequest submitRequest =
                        JsonSupport.mapper()
                                .readValue(
                                        body,
                                        JobSubmitRequest.class);

                result = service.submit(submitRequest);
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException(
                        "Invalid JSON submit request");
            }
        } else {
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
