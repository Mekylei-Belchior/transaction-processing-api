package com.mekylei.transactionprocessing;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.seguranca.HmacService;
import com.mekylei.transactionprocessing.conta.dominio.Conta;
import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.conta.dominio.Saldo;
import com.mekylei.transactionprocessing.conta.dominio.StatusConta;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.ContaJpaAdapter;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.LimiteJpaAdapter;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.SaldoJpaAdapter;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.TransacaoJpaAdapter;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.ContaJpaRepository;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.EventoProcessadoJpaRepository;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.LimiteJpaRepository;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.OutboxEventoJpaRepository;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.SaldoJpaRepository;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.TransacaoJpaRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Classe base para testes de integração com banco PostgreSQL real.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Inicializar um {@link PostgreSQLContainer} compartilhado para a suíte de integração.</li>
 *     <li>Disponibilizar {@link MockMvc}, autenticação JWT mockada e fixtures persistidas com JPA.</li>
 *     <li>Garantir isolamento entre cenários por limpeza explícita das tabelas de domínio.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Registro dinâmico das propriedades de datasource do container PostgreSQL.</li>
 *     <li>Criação de contas, saldos, limites e transações para testes end-to-end.</li>
 *     <li>Geração de JWT com roles compatíveis com a configuração de segurança da aplicação.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Publicação real em Kafka, testes de carga e validações externas ao banco de integração.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class IntegrationTestBase {

    @Container
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("transaction_processing_integration")
            .withUsername("integration")
            .withPassword("integration");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ContaJpaAdapter contaJpaAdapter;

    @Autowired
    protected SaldoJpaAdapter saldoJpaAdapter;

    @Autowired
    protected LimiteJpaAdapter limiteJpaAdapter;

    @Autowired
    protected TransacaoJpaAdapter transacaoJpaAdapter;

    @Autowired
    protected ContaJpaRepository contaJpaRepository;

    @Autowired
    protected SaldoJpaRepository saldoJpaRepository;

    @Autowired
    protected LimiteJpaRepository limiteJpaRepository;

    @Autowired
    protected TransacaoJpaRepository transacaoJpaRepository;

    @Autowired
    protected OutboxEventoJpaRepository outboxEventoJpaRepository;

    @Autowired
    protected EventoProcessadoJpaRepository eventoProcessadoJpaRepository;

    @Autowired
    protected HmacService hmacService;

    @DynamicPropertySource
    static void registraDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
    }

    @BeforeEach
    void limparBanco() {
        eventoProcessadoJpaRepository.deleteAllInBatch();
        outboxEventoJpaRepository.deleteAllInBatch();
        transacaoJpaRepository.deleteAllInBatch();
        limiteJpaRepository.deleteAllInBatch();
        saldoJpaRepository.deleteAllInBatch();
        contaJpaRepository.deleteAllInBatch();
    }

    protected RequestPostProcessor jwtToken(String role) {
        return jwt()
                .jwt(token -> token
                        .subject(UUID.randomUUID().toString())
                        .claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    protected UUID criarContaAtivaComSaldoELimites(BigDecimal saldoInicial, TipoTransacao... tipos) {
        return criarContaAtivaComSaldoELimites(
                saldoInicial,
                new BigDecimal("999999.99"),
                new BigDecimal("999999.99"),
                tipos);
    }

    protected UUID criarContaAtivaComSaldoELimites(BigDecimal saldoInicial,
                                                   BigDecimal limiteDiario,
                                                   BigDecimal limiteTransacao,
                                                   TipoTransacao... tipos) {
        Conta conta = contaJpaAdapter.save(Conta.builder()
                .numeroConta("cc-" + UUID.randomUUID())
                .agencia("0001")
                .idCliente(UUID.randomUUID())
                .tipo(TipoConta.CORRENTE)
                .status(StatusConta.ATIVA)
                .build());

        saldoJpaAdapter.save(Saldo.builder()
                .idConta(conta.getId())
                .disponivel(saldoInicial)
                .bloqueado(BigDecimal.ZERO)
                .build());

        for (TipoTransacao tipo : tipos) {
            limiteJpaAdapter.save(LimiteTransacional.builder()
                    .idConta(conta.getId())
                    .tipo(tipo)
                    .limiteDiario(limiteDiario)
                    .limiteTransacao(limiteTransacao)
                    .utilizadoHoje(BigDecimal.ZERO)
                    .dataReferencia(LocalDate.now())
                    .build());
        }

        return conta.getId();
    }

    protected Transacao criarTransacaoConcluida(UUID idContaOrigem, BigDecimal valor, TipoTransacao tipo) {
        return transacaoJpaAdapter.save(Transacao.builder()
                .idCorrelacao(UUID.randomUUID())
                .idIdempotencia(UUID.randomUUID())
                .valor(ValorMonetario.paraReal(valor))
                .tipo(tipo)
                .status(StatusTransacao.COMPLETADA)
                .idContaOrigem(idContaOrigem)
                .contaDestino("destino-" + UUID.randomUUID())
                .build());
    }

    protected String transacaoRequisicaoJson(BigDecimal valor, UUID idContaOrigem, String contaDestino) {
        return """
                {
                  "valor": %s,
                  "idContaOrigem": "%s",
                  "contaDestino": "%s"
                }
                """.formatted(valor, idContaOrigem, contaDestino);
    }

    protected String estornoRequisicaoJson(String motivo) {
        return """
                {
                  "motivo": "%s"
                }
                """.formatted(motivo);
    }
}
