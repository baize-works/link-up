package com.link.up.server.service;

/** Connector 预检成功响应。失败通过统一 REST 错误协议返回。 */
public final class ConnectorPreflightResponse {

    private final String connectorId;
    private final String role;
    private final String status;
    private final long durationMillis;
    private final long checkedAtMillis;

    public ConnectorPreflightResponse(
            String connectorId,
            String role,
            String status,
            long durationMillis,
            long checkedAtMillis) {

        this.connectorId = connectorId;
        this.role = role;
        this.status = status;
        this.durationMillis = durationMillis;
        this.checkedAtMillis = checkedAtMillis;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public long getCheckedAtMillis() {
        return checkedAtMillis;
    }
}
