package com.kardexa.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hace cumplir las reglas de dependencia del ADR 0002. Si una clase las rompe, el build falla.
 */
@AnalyzeClasses(packages = "com.kardexa", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String[] FRAMEWORKS = {
        "org.springframework..", "jakarta.persistence..", "org.hibernate..", "tools.jackson..", "com.fasterxml.jackson.."
    };

    @ArchTest
    static final ArchRule domainIsPureJava = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .because("el dominio debe poder probarse sin frameworks (ADR 0002, regla 1)");

    @ArchTest
    static final ArchRule domainDoesNotKnowOuterLayers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .because("las dependencias apuntan hacia el dominio, nunca desde él (ADR 0002, regla 1)");

    @ArchTest
    static final ArchRule applicationIsPureJava = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .because("los casos de uso no dependen de Spring ni de JPA (ADR 0002, regla 2)");

    @ArchTest
    static final ArchRule applicationDoesNotKnowInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("la aplicación define puertos; la infraestructura los implementa (ADR 0002, regla 2)");

    @ArchTest
    static final ArchRule jpaEntitiesLiveInPersistenceAdapters = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage("..infrastructure.adapter.out.persistence..")
            .allowEmptyShould(true)
            .because("las entidades JPA son un detalle de infraestructura (ADR 0002, regla 5)");

    @ArchTest
    static final ArchRule controllersLiveInWebAdapters = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().resideInAPackage("..infrastructure.adapter.in.web..")
            .allowEmptyShould(true)
            .because("los controladores son adaptadores de entrada (ADR 0002)");

    // Ojo: se usa classes() y no noClasses(). Con noClasses(), ArchUnit invierte los eventos de una
    // condición propia y las violaciones pasarían como cumplimientos (la regla nunca fallaría).
    @ArchTest
    static final ArchRule modulesTalkOnlyThroughInputPorts = classes()
            .should(onlyAccessOtherModulesThroughInputPorts())
            .because("un módulo solo usa otro a través de sus puertos de entrada (ADR 0002, regla 4)");

    private static final Pattern MODULE = Pattern.compile("^com\\.kardexa\\.([a-z]+)\\..*");

    private static Optional<String> moduleOf(JavaClass clazz) {
        Matcher matcher = MODULE.matcher(clazz.getPackageName() + ".");
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static ArchCondition<JavaClass> onlyAccessOtherModulesThroughInputPorts() {
        return new ArchCondition<>("acceder a otros módulos solo a través de su application.port.in") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> originModule = moduleOf(origin);
                if (originModule.isEmpty() || originModule.get().equals("shared")) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    Optional<String> targetModule = moduleOf(target);
                    boolean crossesModules = targetModule.isPresent()
                            && !targetModule.get().equals("shared")
                            && !targetModule.equals(originModule);
                    if (crossesModules && !target.getPackageName().contains(".application.port.in")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }
}
