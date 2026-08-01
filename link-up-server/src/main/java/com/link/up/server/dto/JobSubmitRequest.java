package com.link.up.server.dto;

/**
 * Link-Up Worker JSON 提交协议。
 */
public final class JobSubmitRequest {

    private String externalExecutionId;
    private String idempotencyKey;
    private Integer definitionVersion;
    private String hocon;

    public JobSubmitRequest() {
    }

    public String getExternalExecutionId() {
        return externalExecutionId;
    }

    public void setExternalExecutionId(
            String externalExecutionId) {
        this.externalExecutionId = externalExecutionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(
            String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Integer getDefinitionVersion() {
        return definitionVersion;
    }

    public void setDefinitionVersion(
            Integer definitionVersion) {
        this.definitionVersion = definitionVersion;
    }

    public String getHocon() {
        return hocon;
    }

    public void setHocon(String hocon) {
        this.hocon = hocon;
    }
}
