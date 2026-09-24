package com.buildingos.building.buildingapplication.application.listapplications;

import com.buildingos.building.buildingapplication.domain.model.ApplicationStatus;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

/** Back-office review queue. Drafts are the applicant's private work and never listed. */
public final class ListApplicationsService implements ListApplicationsUseCase {
    private final BuildingApplicationRepository applications;

    public ListApplicationsService(BuildingApplicationRepository applications) { this.applications = applications; }

    @Override
    public Page<BuildingApplication> execute(Actor actor, ListApplicationsQuery query) {
        actor.requirePlatformAdmin();
        Page.validate(query.page(), query.size());
        if (query.status() == ApplicationStatus.DRAFT) {
            throw new IllegalArgumentException("Draft applications are not listed");
        }
        var items = applications.findPage(query.status(), query.page() * query.size(), query.size());
        return new Page<>(items, query.page(), query.size(), applications.count(query.status()));
    }
}
