package com.mekylei.transactionprocessing.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Testes arquiteturais para isolar a camada de segurança.
 *
 * <p>As regras impedem que configuração de segurança se espalhe pelo código e que o domínio passe
 * a depender de anotações de autorização.</p>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@AnalyzeClasses(packages = "com.mekylei.transactionprocessing", importOptions = ImportOption.DoNotIncludeTests.class)
public class CamadaSegurancaTest {

    /**
     * Verifica que configurações que referenciam SecurityFilterChain ficam no pacote de segurança.
     * Isso concentra a configuração HTTP/Spring Security em uma área arquitetural previsível.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule configuracoesSegurancaIsoladas =
            classes()
                    .that(configuracoesQueReferenciamSecurityFilterChain())
                    .should()
                    .resideInAPackage("..configuracao.seguranca..")
                    .because("SecurityFilterChain é configuração de segurança e deve ficar isolado nesse pacote");

    /**
     * Verifica que classes de domínio não usam anotações de segurança.
     * Regras de autorização pertencem às bordas ou à aplicação, não ao modelo de domínio.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule semAnotacoesSegurancaNoDominio =
            noClasses()
                    .that()
                    .resideInAPackage("..dominio..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.security.access.prepost..",
                            "org.springframework.security.access.annotation..",
                            "jakarta.annotation.security..")
                    .because("o domínio deve permanecer livre de decisões de autorização e frameworks de segurança");

    private static DescribedPredicate<JavaClass> configuracoesQueReferenciamSecurityFilterChain() {
        return new DescribedPredicate<>("configurações que referenciam SecurityFilterChain") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isAnnotatedWith(Configuration.class)
                        && javaClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> "org.springframework.security.web.SecurityFilterChain"
                                .equals(dependency.getTargetClass().getName()));
            }
        };
    }
}
