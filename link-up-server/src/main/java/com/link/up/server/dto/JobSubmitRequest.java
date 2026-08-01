package com.link.up.server.dto;

import com.link.up.api.job.JobSpec;

/** Link-Up Worker idempotent JSON submission protocol. */
public final class JobSubmitRequest {
    private String externalExecutionId;
    private String idempotencyKey;
    private Integer definitionVersion;
    private JobSpec jobSpec;
    private String hocon;

    public JobSubmitRequest() {}
    public String getExternalExecutionId() { return externalExecutionId; }
    public void setExternalExecutionId(String value) { this.externalExecutionId = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public Integer getDefinitionVersion() { return definitionVersion; }
    public void setDefinitionVersion(Integer value) { this.definitionVersion = value; }
    public JobSpec getJobSpec() { return jobSpec; }
    public void setJobSpec(JobSpec jobSpec) { this.jobSpec = jobSpec; }
    public String getHocon() { return hocon; }
    public void setHocon(String hocon) { this.hocon = hocon; }
}
