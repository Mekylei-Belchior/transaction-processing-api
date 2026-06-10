package com.mekylei.transactionprocessing.arquitetura;

import com.mekylei.transactionprocessing.mensageria.outbox.DominioEventoOutboxPublicador;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.servlet.Filter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Testes arquiteturais para convenções de nomenclatura do projeto.
 *
 * <p>As regras tornam explícitos os sufixos esperados para adapters, repositories, entities,
 * services, controllers e filters, facilitando leitura e descoberta por responsabilidade.</p>
 *
 * <p>Débitos técnicos conhecidos:</p>
 * <ul>
 *     <li>ARQ-TECH-DEBT-003: {@link DominioEventoOutboxPublicador} está anotado com {@code @Service},
 *     mas não usa o sufixo Service. A correção recomendada é renomear a classe ou trocar a
 *     estereotipagem para um componente mais adequado ao papel de publicador.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@AnalyzeClasses(packages = "com.mekylei.transactionprocessing", importOptions = ImportOption.DoNotIncludeTests.class)
public class NamingConventionTest {

    /**
     * Verifica que classes de persistência concreta usam o sufixo JpaAdapter.
     * Esse padrão diferencia adapters de portas e repositories Spring Data.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule nomenclaturaAdaptersJpa =
            classes()
                    .that()
                    .resideInAPackage("..infraestrutura.persistencia..")
                    .should()
                    .haveSimpleNameEndingWith("JpaAdapter")
                    .because("adapters JPA devem explicitar a tecnologia e o papel arquitetural no nome");

    /**
     * Verifica que repositories Spring Data JPA usam o sufixo JpaRepository.
     * Isso separa interfaces técnicas de persistência das portas de aplicação.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule nomenclaturaRepositoriesJpa =
            classes()
                    .that(estendemJpaRepository())
                    .should()
                    .haveSimpleNameEndingWith("JpaRepository")
                    .because("repositories Spring Data JPA devem ser identificáveis pelo sufixo JpaRepository");

    /**
     * Verifica que entidades JPA usam o sufixo Entity.
     * O sufixo deixa claro que a classe é um modelo de persistência, não uma entidade de domínio.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule nomenclaturaEntities =
            classes()
                    .that()
                    .areAnnotatedWith(Entity.class)
                    .should()
                    .haveSimpleNameEndingWith("Entity")
                    .because("classes anotadas com @Entity pertencem ao modelo de persistência");

    /**
     * Verifica que componentes de serviço Spring usam o sufixo Service.
     * O padrão ajuda a reconhecer casos de uso e serviços transversais registrados no container.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule nomenclaturaServices =
            classes()
                    .that()
                    .areAnnotatedWith(Service.class)
                    .and()
                    // TODO [TECH-DEBT]: classe anotada com @Service sem sufixo Service; renomear quando aprovado.
                    .doNotHaveSimpleName("DominioEventoOutboxPublicador")
                    .should()
                    .haveSimpleNameEndingWith("Service")
                    .because("classes anotadas com @Service devem declarar esse papel no nome");

    /**
     * Verifica que controllers REST usam o sufixo Controller.
     * O padrão mantém a borda HTTP fácil de localizar.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule nomenclaturaControllers =
            classes()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .haveSimpleNameEndingWith("Controller")
                    .because("controllers REST devem ser reconhecidos pelo sufixo Controller");

    /**
     * Verifica que filtros Servlet usam o sufixo Filter.
     * O padrão torna explícita a participação no pipeline HTTP.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule nomenclaturaFilters =
            classes()
                    .that()
                    .areAssignableTo(Filter.class)
                    .should()
                    .haveSimpleNameEndingWith("Filter")
                    .because("implementações de jakarta.servlet.Filter devem declarar esse papel no nome");

    private static DescribedPredicate<JavaClass> estendemJpaRepository() {
        return new DescribedPredicate<>("estendem JpaRepository") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getAllRawInterfaces().stream()
                        .anyMatch(interfaceType -> "org.springframework.data.jpa.repository.JpaRepository"
                                .equals(interfaceType.getName()));
            }
        };
    }
}
