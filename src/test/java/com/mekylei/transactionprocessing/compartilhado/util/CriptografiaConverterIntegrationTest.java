package com.mekylei.transactionprocessing.compartilhado.util;

import com.mekylei.transactionprocessing.infraestrutura.entidade.ContaBancariaEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.ContaBancariaTestRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Testes de integração para CriptografiaConverter com JPA/Hibernate real.
 *
 * Objetivo: validar que o converter atua corretamente na camada de persistência —
 * que dados são efetivamente criptografados antes de serem gravados no banco e
 * transparentemente descriptografados ao serem lidos pela entidade JPA.
 *
 * Configuração:
 *  - @DataJpaTest: contexto JPA restrito sem beans de serviço ou controller.
 *  - @AutoConfigureTestDatabase(replace = Replace.NONE): utiliza o datasource H2
 *    configurado em application-test.yml com MODE=PostgreSQL para compatibilidade
 *    com columnDefinition "jsonb" presente em outras entidades do projeto.
 *  - @Import(CriptografiaConverter.class): torna o converter disponível como
 *    bean Spring no contexto JPA de teste, necessário porque @DataJpaTest não
 *    carrega @Component genéricos automaticamente.
 *  - JdbcTemplate: consultas nativas para inspecionar o valor raw da coluna e
 *    confirmar que o plaintext jamais é persistido em texto claro.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@Import(CriptografiaConverter.class)
@TestPropertySource(properties = "app.criptografia.chave=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@DisplayName("CriptografiaConverter — Integração JPA")
class CriptografiaConverterIntegrationTest {

    @Autowired
    private ContaBancariaTestRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // Persistência — gravação
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ao persistir entidade com campos criptografados")
    class AoPersistir {

        @Test
        @DisplayName("deve salvar a entidade sem erros e retornar ID gerado")
        void deve_salvar_entidade_sem_erros() {
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), "0001-98765-4", "João da Silva");

            ContaBancariaEntity salva = repository.save(conta);

            assertThat(salva.getId()).isNotNull();
        }

        @Test
        @DisplayName("valor raw na coluna do banco NÃO deve conter o plaintext de numeroConta")
        void coluna_numero_conta_nao_deve_armazenar_plaintext() {
            String numeroContaPlaintext = "0001-12345-9";
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), numeroContaPlaintext, "Maria Oliveira");
            repository.save(conta);
            entityManager.flush();

            String valorRaw = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class,
                    conta.getId());

            assertThat(valorRaw).isNotNull();
            assertThat(valorRaw).isNotEqualTo(numeroContaPlaintext);
            assertThat(valorRaw).doesNotContain(numeroContaPlaintext);
        }

        @Test
        @DisplayName("valor raw na coluna do banco NÃO deve conter o plaintext de titular")
        void coluna_titular_nao_deve_armazenar_plaintext() {
            String titularPlaintext = "CPF:987.654.321-00 — Carlos Pereira";
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), "0002-55555-7", titularPlaintext);
            repository.save(conta);
            entityManager.flush();

            String valorRaw = jdbcTemplate.queryForObject(
                    "SELECT titular FROM conta_bancaria_test WHERE id = ?",
                    String.class,
                    conta.getId());

            assertThat(valorRaw).isNotNull();
            assertThat(valorRaw).doesNotContain(titularPlaintext);
        }

        @Test
        @DisplayName("valor raw armazenado deve ser um Base64 válido (formato criptografado esperado)")
        void valor_raw_deve_ser_base64_valido() {
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), "0001-77777-2", "Ana Souza");
            repository.save(conta);
            entityManager.flush();

            String valorRaw = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class,
                    conta.getId());

            assertThatNoException()
                    .isThrownBy(() -> Base64.getDecoder().decode(valorRaw));
        }

        @Test
        @DisplayName("payload raw deve ter tamanho mínimo de IV + ciphertext + tag GCM em Base64")
        void payload_raw_deve_ter_tamanho_minimo() {
            String numeroConta = "0003-11111-5";
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), numeroConta, "Pedro Lima");
            repository.save(conta);
            entityManager.flush();

            String valorRaw = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class,
                    conta.getId());

            byte[] decodificado = Base64.getDecoder().decode(valorRaw);
            int tamanhoMinimo = 12 + numeroConta.getBytes().length + 16;
            assertThat(decodificado.length).isGreaterThanOrEqualTo(tamanhoMinimo);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Leitura — descriptografia transparente pelo JPA
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ao carregar entidade do banco")
    class AoCarregar {

        @Test
        @DisplayName("deve retornar numeroConta descriptografado ao buscar por ID")
        void deve_retornar_numero_conta_descriptografado() {
            String numeroConta = "0001-88888-3";
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), numeroConta, "Fernanda Costa");
            repository.save(conta);
            entityManager.flush();
            entityManager.clear();

            Optional<ContaBancariaEntity> encontrada = repository.findById(conta.getId());

            assertThat(encontrada).isPresent();
            assertThat(encontrada.get().getNumeroConta()).isEqualTo(numeroConta);
        }

        @Test
        @DisplayName("deve retornar titular descriptografado ao buscar por ID")
        void deve_retornar_titular_descriptografado() {
            String titular = "Ricardo Alves — CPF:111.222.333-44";
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), "0001-66666-8", titular);
            repository.save(conta);
            entityManager.flush();
            entityManager.clear();

            Optional<ContaBancariaEntity> encontrada = repository.findById(conta.getId());

            assertThat(encontrada).isPresent();
            assertThat(encontrada.get().getTitular()).isEqualTo(titular);
        }

        @Test
        @DisplayName("findById deve retornar Optional vazio para ID inexistente")
        void deve_retornar_optional_vazio_para_id_inexistente() {
            Optional<ContaBancariaEntity> resultado = repository.findById(UUID.randomUUID());

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("findAll deve retornar todas as entidades com campos descriptografados")
        void deve_retornar_todas_as_entidades_com_campos_descriptografados() {
            ContaBancariaEntity conta1 = new ContaBancariaEntity(
                    UUID.randomUUID(), "0001-11111-1", "Titular Um");
            ContaBancariaEntity conta2 = new ContaBancariaEntity(
                    UUID.randomUUID(), "0002-22222-2", "Titular Dois");
            repository.save(conta1);
            repository.save(conta2);
            entityManager.flush();
            entityManager.clear();

            var todas = repository.findAll();

            assertThat(todas).hasSize(2);
            assertThat(todas)
                    .extracting(ContaBancariaEntity::getNumeroConta)
                    .containsExactlyInAnyOrder("0001-11111-1", "0002-22222-2");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Atualização — criptografia mantida após update
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ao atualizar entidade com campos criptografados")
    class AoAtualizar {

        @Test
        @DisplayName("deve criptografar o novo valor de numeroConta após update")
        void deve_criptografar_novo_valor_apos_update() {
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), "0001-99999-0", "Luiza Martins");
            repository.save(conta);
            entityManager.flush();

            conta.setNumeroConta("0001-00000-1");
            repository.save(conta);
            entityManager.flush();

            String valorRawAtualizado = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class,
                    conta.getId());

            assertThat(valorRawAtualizado).isNotEqualTo("0001-00000-1");
            assertThatNoException()
                    .isThrownBy(() -> Base64.getDecoder().decode(valorRawAtualizado));
        }

        @Test
        @DisplayName("deve retornar novo valor descriptografado após update e reload")
        void deve_retornar_novo_valor_descriptografado_apos_update() {
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), "0001-55555-5", "Marcos Ribeiro");
            repository.save(conta);
            entityManager.flush();

            conta.setNumeroConta("0001-44444-4");
            conta.setTitular("Marcos Ribeiro Atualizado");
            repository.save(conta);
            entityManager.flush();
            entityManager.clear();

            ContaBancariaEntity recarregada = repository.findById(conta.getId()).orElseThrow();

            assertThat(recarregada.getNumeroConta()).isEqualTo("0001-44444-4");
            assertThat(recarregada.getTitular()).isEqualTo("Marcos Ribeiro Atualizado");
        }

        @Test
        @DisplayName("criptografias antes e após update devem ser distintas (novo IV a cada operação)")
        void criptografias_antes_e_apos_update_devem_ser_distintas() {
            // Insere entidade original
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), "0001-33333-3", "Clara Nunes");
            repository.save(conta);
            entityManager.flush();

            String valorRawAntes = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class,
                    conta.getId());

            // Limpa o contexto para garantir estado fresco, muda para um valor distinto,
            // depois volta ao valor original — duas chamadas ao converter com mesmo plaintext
            entityManager.clear();
            ContaBancariaEntity recarregada = repository.findById(conta.getId()).orElseThrow();
            recarregada.setNumeroConta("TEMP-00000-0");
            repository.save(recarregada);
            entityManager.flush();

            recarregada.setNumeroConta("0001-33333-3");
            repository.save(recarregada);
            entityManager.flush();

            String valorRawDepois = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class,
                    conta.getId());

            assertThat(valorRawAntes).isNotEqualTo(valorRawDepois);
        }

        @Test
        @DisplayName("deve suportar update para valor com caracteres especiais e acentos")
        void deve_suportar_update_com_caracteres_especiais() {
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), "0001-22222-2", "Inicial");
            repository.save(conta);
            entityManager.flush();

            String novoTitular = "Ação Bancária — Ônus Específico 💳";
            conta.setTitular(novoTitular);
            repository.save(conta);
            entityManager.flush();
            entityManager.clear();

            ContaBancariaEntity recarregada = repository.findById(conta.getId()).orElseThrow();

            assertThat(recarregada.getTitular()).isEqualTo(novoTitular);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consistência — separação entre valores raw e desencriptados
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Consistência entre valor no banco e valor na entidade")
    class Consistencia {

        @Test
        @DisplayName("valor raw no banco e valor via JPA devem ser sempre distintos")
        void valor_raw_e_valor_jpa_devem_ser_distintos() {
            String numeroConta = "0001-12121-2";
            ContaBancariaEntity conta = new ContaBancariaEntity(
                    UUID.randomUUID(), numeroConta, "Teste Consistência");
            repository.save(conta);
            entityManager.flush();
            entityManager.clear();

            String valorRaw = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class,
                    conta.getId());
            ContaBancariaEntity recarregada = repository.findById(conta.getId()).orElseThrow();

            assertThat(valorRaw).isNotEqualTo(numeroConta);
            assertThat(recarregada.getNumeroConta()).isEqualTo(numeroConta);
            assertThat(valorRaw).isNotEqualTo(recarregada.getNumeroConta());
        }

        @Test
        @DisplayName("dois saves do mesmo plaintext devem gerar valores raw distintos no banco")
        void dois_saves_do_mesmo_plaintext_devem_gerar_raw_distintos() {
            String numeroConta = "0001-77777-7";
            ContaBancariaEntity conta1 = new ContaBancariaEntity(
                    UUID.randomUUID(), numeroConta, "Titular A");
            ContaBancariaEntity conta2 = new ContaBancariaEntity(
                    UUID.randomUUID(), numeroConta, "Titular B");
            repository.save(conta1);
            repository.save(conta2);
            entityManager.flush();

            String rawConta1 = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class, conta1.getId());
            String rawConta2 = jdbcTemplate.queryForObject(
                    "SELECT numero_conta FROM conta_bancaria_test WHERE id = ?",
                    String.class, conta2.getId());

            assertThat(rawConta1).isNotEqualTo(rawConta2);
        }
    }
}
