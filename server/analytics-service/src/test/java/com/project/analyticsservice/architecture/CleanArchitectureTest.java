package com.project.analyticsservice.architecture;

import com.project.analyticsservice.AnalyticsServiceApplication;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CleanArchitectureTest {
    private final JavaClasses classes;

    CleanArchitectureTest() {
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
        classes = new ClassFileImporter().importPackagesOf(AnalyticsServiceApplication.class);
    }

    @Test
    void controllersDoNotReachDomainPersistenceOrEntities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..repository..", "..entity..", "..domain..");
        rule.check(classes);
    }

    @Test
    void applicationServicesDoNotReachRepositoriesOrEntities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..repository..", "..entity..");
        rule.check(classes);
    }

    @Test
    void repositoriesContainNoSqlCalculationQueries() {
        classes.stream()
                .filter(javaClass -> javaClass.getPackageName().contains(".repository"))
                .flatMap(javaClass -> javaClass.getMethods().stream())
                .forEach(method -> assertFalse(
                        method.isAnnotatedWith(Query.class),
                        () -> method.getFullName() + " must remain CRUD-only"));
    }
}
