package com.jbetting.betting.unit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.jbetting.betting", importOptions = ImportOption.DoNotIncludeTests.class)
class BettingArchitectureTest {

    @ArchTest
    static final ArchRule le_coeur_ne_depend_d_aucun_framework =
            noClasses().that().resideInAPackage("..businesslogic..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta..", "org.hibernate..", "com.fasterxml.jackson..", "lombok..");

    @ArchTest
    static final ArchRule le_coeur_ignore_ce_qui_l_entoure =
            noClasses().that().resideInAPackage("..businesslogic..")
                    .should().dependOnClassesThat().resideInAnyPackage("..adapters..", "..configuration..");

    @ArchTest
    static final ArchRule le_coeur_ne_lit_jamais_l_heure_systeme =
            noClasses().that().resideInAPackage("..businesslogic..")
                    .should().callMethodWhere(target(name("now")).and(target(owner(resideInAPackage("java.time")))));
}
