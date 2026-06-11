package com.mekylei.transactionprocessing.arquitetura;

import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.dependencies.Slice;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Testes arquiteturais que documentam e protegem as decisões de Arquitetura Hexagonal,
 * Clean Architecture e DDD do projeto.
 *
 * <p>As regras garantem que domínio, aplicação, adapters e bordas HTTP mantenham dependências
 * coerentes com o fluxo de dependência de fora para dentro.</p>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@AnalyzeClasses(packages = "com.mekylei.transactionprocessing", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArquiteturaHexagonalTest {

    /**
     * Verifica que o domínio permanece puro e não conhece infraestrutura, configuração ou controle.
     * Essa regra preserva o centro da arquitetura independente de Spring, JPA e adapters externos.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule dominioPuroSemDependenciaDeInfra =
            noClasses()
                    .that()
                    .resideInAnyPackage("..transacao.dominio..", "..conta.dominio..", "..compartilhado.dominio..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..infraestrutura..", "..configuracao..", "..controle..")
                    .because("o domínio não deve conhecer detalhes de infraestrutura, configuração ou entrada HTTP");

    /**
     * Verifica que serviços de aplicação dependem de portas, e não de adapters JPA ou repositórios Spring.
     * Isso mantém a orquestração de casos de uso independente da persistência concreta.
     *
     * <p>Débitos técnicos conhecidos: nenhum débito registrado nesta regra.</p>
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule servicosAplicacaoDependemApenasDePortas =
            noClasses()
                    .that()
                    .resideInAnyPackage("..transacao.aplicacao.servico..", "..conta.aplicacao.servico..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..infraestrutura.persistencia..", "..infraestrutura.repositorio..")
                    .because("serviços de aplicação devem depender de interfaces de porta, nunca de adapters concretos");

    /**
     * Verifica que controllers REST dependem da camada de aplicação, DTOs da borda e value objects permitidos.
     * Referências a classes de domínio do mesmo bounded context são permitidas quando representam
     * o contrato atual dos serviços de aplicação; infraestrutura segue proibida na borda HTTP.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule controllersDependemApenasDaAplicacao =
            classes()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should(naoDependerDeInfraOuDominioExcetoMesmoBoundedContext())
                    .because("controllers devem atuar como borda HTTP fina e chamar serviços de aplicação");

    /**
     * Verifica que implementações de portas Repository ou Gateway ficam em infraestrutura ou integração.
     * Essa regra evita que adapters concretos escapem para domínio ou aplicação.
     * Portas de contexto interno, como ContextGateway e ContextWriter, não representam integração
     * com recurso externo e ficam fora do escopo desta verificação.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule adaptadoresJpaNaInfraestrutura =
            classes()
                    .that(implementamPortaRepositoryOuGateway())
                    .should()
                    .resideInAnyPackage("..infraestrutura..", "..integracao..")
                    .because("implementações de portas são adapters externos e pertencem às camadas de infraestrutura ou integração");

    /**
     * Verifica que entidades JPA ficam isoladas no pacote de entidade da infraestrutura.
     * Classes de domínio não devem receber anotações JPA para evitar acoplamento com persistência.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule entidadesJpaIsoladasNaInfraestrutura =
            classes()
                    .that()
                    .areAnnotatedWith(Entity.class)
                    .should()
                    .resideInAPackage("..infraestrutura.entidade..")
                    .because("@Entity é detalhe de persistência e deve ficar isolado na infraestrutura");

    /**
     * Verifica que strategies de transação implementam a interface de strategy correta.
     * Isso preserva o contrato polimórfico usado pelo resolvedor de estratégias.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule strategiesImplementamInterfaceCorreta =
            classes()
                    .that()
                    .resideInAPackage("..transacao.estrategia..")
                    .and()
                    .haveSimpleNameEndingWith("Strategy")
                    .and()
                    .doNotHaveSimpleName("TransacaoStrategy")
                    .should()
                    .beAssignableTo(TransacaoStrategy.class)
                    .because("toda strategy concreta deve obedecer ao contrato TransacaoStrategy");

    /**
     * Verifica a Dependency Rule da Clean Architecture.
     * O domínio não depende de aplicação, infraestrutura ou controle; a aplicação não depende de infraestrutura.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule dependencyRuleCleanArchitecture =
            CompositeArchRule.of(
                            noClasses()
                                    .that()
                                    .resideInAPackage("..dominio..")
                                    .should()
                                    .dependOnClassesThat()
                                    .resideInAnyPackage("..aplicacao..", "..infraestrutura..", "..controle.."))
                    .and(noClasses()
                            .that()
                            .resideInAPackage("..aplicacao..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage("..infraestrutura.."))
                    .because("a dependência deve fluir de fora para dentro: controle -> aplicação -> domínio");

    /**
     * Verifica que não existem dependências cíclicas entre os pacotes principais do sistema.
     * Ciclos entre bounded contexts dificultam evolução independente e deixam regras de negócio espalhadas.
     * A dependência de infraestrutura para mensageria.outbox é intencional: o adapter JPA implementa
     * portas do outbox seguindo a Dependency Rule.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule semCiclosEntrePacotesPrincipais =
            slices()
                    .matching("com.mekylei.transactionprocessing.(*)..")
                    .namingSlices("$1")
                    .that(saoBoundedContextsPrincipais())
                    .should()
                    .beFreeOfCycles()
                    .ignoreDependency(
                            resideInAPackage("..infraestrutura.."),
                            resideInAPackage("..mensageria.outbox..")
                    );

    /**
     * Verifica que value objects compartilhados são records ou classes finais.
     * Essa regra protege a imutabilidade dos objetos de valor usados pelos contextos de domínio.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule valueObjectsSaoRecordsOuClassesFinais =
            classes()
                    .that()
                    .resideInAPackage("..compartilhado.dominio..")
                    .should(seremRecordsOuClassesFinais())
                    .because("value objects devem ser imutáveis e seguros para compartilhamento");

    /**
     * Verifica que handlers globais de exceção ficam em pacotes compartilhados ou de configuração.
     * Isso evita que tratamento transversal de erro seja definido dentro de contextos específicos.
     *
     * @author Mekylei Belchior
     * @since 1.0
     */
    @ArchTest
    public static final ArchRule handlersExcecaoGlobaisIsolados =
            classes()
                    .that()
                    .areAnnotatedWith(RestControllerAdvice.class)
                    .should()
                    .resideInAnyPackage("..compartilhado.exception..", "..configuracao..")
                    .because("handlers globais de exceção são preocupação transversal");

    private static DescribedPredicate<JavaClass> implementamPortaRepositoryOuGateway() {
        return new DescribedPredicate<>("implementam interfaces de porta com sufixo Repository ou Gateway (exceto Context)") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getAllRawInterfaces().stream()
                        .anyMatch(interfaceType -> {
                            String nome = interfaceType.getSimpleName();
                            boolean ehPortaExterna = nome.endsWith("Repository") || nome.endsWith("Gateway");
                            boolean ehPortaContexto = nome.contains("Context");
                            return ehPortaExterna && !ehPortaContexto;
                        });
            }
        };
    }

    private static DescribedPredicate<Slice> saoBoundedContextsPrincipais() {
        return new DescribedPredicate<>("representam transacao, conta, auditoria, mensageria, configuracao ou infraestrutura") {
            @Override
            public boolean test(Slice slice) {
                return nomeSlicePrincipal(slice, 0) || nomeSlicePrincipal(slice, 1);
            }
        };
    }

    private static boolean nomeSlicePrincipal(Slice slice, int indice) {
        try {
            return boundedContextPrincipal(slice.getNamePart(indice));
        } catch (RuntimeException indiceInexistente) {
            return false;
        }
    }

    private static boolean boundedContextPrincipal(String nome) {
        return "transacao".equals(nome)
                || "conta".equals(nome)
                || "auditoria".equals(nome)
                || "mensageria".equals(nome)
                || "configuracao".equals(nome)
                || "infraestrutura".equals(nome);
    }

    private static ArchCondition<JavaClass> naoDependerDeInfraOuDominioExcetoMesmoBoundedContext() {
        return new ArchCondition<>("não depender de infraestrutura ou domínio diretamente") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> dependenciaParaInfraOuDominio(dependency.getTargetClass()))
                        .filter(dependency -> !valueObjectCompartilhado(dependency.getTargetClass()))
                        .filter(dependency -> !mesmoBoundedContext(javaClass, dependency.getTargetClass()))
                        .forEach(dependency -> events.add(new SimpleConditionEvent(
                                javaClass,
                                false,
                                javaClass.getName() + " depende diretamente de "
                                        + dependency.getTargetClass().getName())));
            }
        };
    }

    private static boolean dependenciaParaInfraOuDominio(JavaClass javaClass) {
        String pacote = javaClass.getPackageName();
        return pacote.contains(".infraestrutura") || pacote.contains(".dominio");
    }

    private static boolean valueObjectCompartilhado(JavaClass javaClass) {
        return javaClass.getPackageName().contains(".compartilhado.dominio");
    }

    private static boolean mesmoBoundedContext(JavaClass origem, JavaClass destino) {
        String bcOrigem = extrairBoundedContext(origem.getPackageName());
        String bcDestino = extrairBoundedContext(destino.getPackageName());
        return bcOrigem != null && bcOrigem.equals(bcDestino);
    }

    private static String extrairBoundedContext(String pacote) {
        String prefixo = "com.mekylei.transactionprocessing.";
        if (!pacote.startsWith(prefixo)) {
            return null;
        }

        String restante = pacote.substring(prefixo.length());
        int idx = restante.indexOf('.');
        return idx >= 0 ? restante.substring(0, idx) : restante;
    }

    private static ArchCondition<JavaClass> seremRecordsOuClassesFinais() {
        return new ArchCondition<>("ser records ou classes finais") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean finalClass = javaClass.getModifiers().contains(JavaModifier.FINAL);
                String message = javaClass.getName() + " deve ser record ou classe final";
                events.add(new SimpleConditionEvent(javaClass, finalClass, message));
            }
        };
    }
}
