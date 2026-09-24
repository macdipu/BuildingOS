package com.buildingos.building.buildingapplication.application;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

/** Who may act on an application. Someone else's application is reported as not found, never as forbidden. */
public enum ApplicationAccess {
    APPLICANT {
        @Override
        public boolean allows(Actor actor, BuildingApplication application) {
            return application.isOwnedBy(actor.userId());
        }
    },
    APPLICANT_OR_PLATFORM_ADMIN {
        @Override
        public boolean allows(Actor actor, BuildingApplication application) {
            return application.isOwnedBy(actor.userId()) || actor.isPlatformAdmin();
        }
    },
    PLATFORM_ADMIN {
        @Override
        public boolean allows(Actor actor, BuildingApplication application) {
            return actor.isPlatformAdmin();
        }
    };

    public abstract boolean allows(Actor actor, BuildingApplication application);
}
