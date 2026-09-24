package com.buildingos.auth.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Enforces BRD §10-12 Clean Architecture layering and microservice isolation (BOS-010 TASK-004). */
@AnalyzeClasses(packages = "com.buildingos.auth", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    private static final String ROOT = "com.buildingos.auth";

    @ArchTest
    static final ArchRule domainDependsOnlyOnJavaAndDomain = classes()
            .that().resideInAPackage(ROOT + "..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", ROOT + "..domain..");

    @ArchTest
    static final ArchRule applicationIsFrameworkAndAdapterFree = noClasses()
            .that().resideInAPackage(ROOT + "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + "..infrastructure..", ROOT + "..presentation..",
                    "org.springframework..", "java.sql..", "javax.sql..", "jakarta..", "com.nimbusds..");

    @ArchTest
    static final ArchRule presentationUsesUseCasesNotAdapters = noClasses()
            .that().resideInAPackage(ROOT + "..presentation..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + "..infrastructure..", ROOT + "..domain.repository..", ROOT + "..application.port.out..");

    @ArchTest
    static final ArchRule presentationNeverSeesUseCaseImplementations = noClasses()
            .that().resideInAPackage(ROOT + "..presentation..")
            .should().dependOnClassesThat(resideInAPackage(ROOT + "..application..")
                    .and(simpleNameEndingWith("Service")));

    @ArchTest
    static final ArchRule noLayerFirstPackagesAtServiceRoot = noClasses()
            .should().resideInAnyPackage(
                    ROOT + ".controllers..", ROOT + ".services..", ROOT + ".repositories..", ROOT + ".entities..");

    @ArchTest
    static final ArchRule neverDependsOnBuildingService = noClasses()
            .should().dependOnClassesThat().resideInAPackage("com.buildingos.building..");

    @ArchTest
    static final ArchRule sharesOnlyPlatformWebKernel = noClasses()
            .should().dependOnClassesThat(resideInAPackage("com.buildingos..")
                    .and(not(resideInAnyPackage(ROOT + "..", "com.buildingos.platform.web.."))));
}
