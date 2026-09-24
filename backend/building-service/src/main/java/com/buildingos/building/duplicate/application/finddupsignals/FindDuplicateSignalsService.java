package com.buildingos.building.duplicate.application.finddupsignals;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.duplicate.domain.model.DuplicateMatch;
import com.buildingos.building.duplicate.domain.model.DuplicateMatcher;
import com.buildingos.building.duplicate.domain.repository.DuplicateCandidateRepository;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public final class FindDuplicateSignalsService implements FindDuplicateSignalsUseCase {
    private final ApplicationChanges applications;
    private final DuplicateCandidateRepository candidates;
    private final DuplicateMatcher matcher;

    public FindDuplicateSignalsService(ApplicationChanges applications, DuplicateCandidateRepository candidates,
            DuplicateMatcher matcher) {
        this.applications = applications;
        this.candidates = candidates;
        this.matcher = matcher;
    }

    @Override
    public List<DuplicateMatch> execute(Actor actor, FindDuplicateSignalsQuery query) {
        var application = applications.load(actor, query.applicationId(), ApplicationAccess.PLATFORM_ADMIN);
        var details = application.details();
        var found = candidates.findCandidates(application.id(), details.district(),
                details.contactPhone() == null ? null : details.contactPhone().value(), details.coordinates(),
                matcher.radiusMeters());
        return matcher.match(details, found);
    }
}
