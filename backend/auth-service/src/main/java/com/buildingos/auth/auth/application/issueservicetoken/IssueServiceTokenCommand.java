package com.buildingos.auth.auth.application.issueservicetoken;

public record IssueServiceTokenCommand(String clientId, String clientSecret) {
    @Override
    public String toString() {
        return "IssueServiceTokenCommand[clientId=" + clientId + "]";
    }
}
