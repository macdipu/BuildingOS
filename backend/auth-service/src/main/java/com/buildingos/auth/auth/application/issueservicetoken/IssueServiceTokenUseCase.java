package com.buildingos.auth.auth.application.issueservicetoken;

public interface IssueServiceTokenUseCase {
    IssuedServiceToken execute(IssueServiceTokenCommand command);
}
