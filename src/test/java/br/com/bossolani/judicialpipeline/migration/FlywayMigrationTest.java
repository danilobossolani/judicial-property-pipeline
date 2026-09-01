package br.com.bossolani.judicialpipeline.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationTest {

    @Test
    void deveAplicarTodasAsMigracoesEmBancoVazio()
            throws Exception {

        String url =
                "jdbc:h2:mem:flyway_migration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        Flyway flyway =
                configurarFlyway(
                        url
                );

        MigrateResult resultado =
                flyway.migrate();

        assertEquals(
                4,
                resultado.migrationsExecuted
        );

        try (Connection conexao =
                     DriverManager.getConnection(url, "sa", "");
             Statement consulta =
                     conexao.createStatement()) {

            assertTrue(
                    colunaExiste(
                            consulta,
                            "LEILOES",
                            "VALOR_AVALIACAO_FONTE"
                    )
            );
            assertTrue(
                    colunaExiste(
                            consulta,
                            "RESULTADOS_LOTE_DESCOBERTA",
                            "FONTE"
                    )
            );
            assertEquals(
                    1,
                    consulta.executeUpdate("""
                            INSERT INTO processos (numero_processo)
                            VALUES ('12345671220268261234')
                            """)
            );
        }
    }

    @Test
    void deveCorrigirOrigemLegadaSomenteParaImovelExclusivoDaMega()
            throws Exception {

        String url =
                "jdbc:h2:mem:flyway_legacy_source_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        Flyway ateVersaoDois =
                Flyway.configure()
                        .dataSource(url, "sa", "")
                        .locations("classpath:db/migration")
                        .target(
                                MigrationVersion.fromVersion("2")
                        )
                        .load();

        ateVersaoDois.migrate();

        try (Connection conexao =
                     DriverManager.getConnection(url, "sa", "");
             Statement comando =
                     conexao.createStatement()) {

            inserirCenarioLegado(
                    comando
            );
        }

        configurarFlyway(
                url
        ).migrate();

        try (Connection conexao =
                     DriverManager.getConnection(url, "sa", "");
             Statement consulta =
                     conexao.createStatement()) {

            assertEquals(
                    "Mega Leilões",
                    consultarOrigem(
                            consulta,
                            1
                    )
            );
            assertEquals(
                    "Sublime Leilões",
                    consultarOrigem(
                            consulta,
                            2
                    )
            );
        }
    }

    private Flyway configurarFlyway(
            String url
    ) {

        return Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load();
    }

    private void inserirCenarioLegado(
            Statement comando
    ) throws Exception {

        comando.executeUpdate("""
                INSERT INTO processos (id, numero_processo)
                VALUES (1, 'legado-1'),
                       (2, 'legado-2')
                """);
        comando.executeUpdate("""
                INSERT INTO imoveis (id, processo_id)
                VALUES (1, 1), (2, 2)
                """);
        comando.executeUpdate("""
                INSERT INTO leiloes (id, imovel_id)
                VALUES (1, 1), (2, 2), (3, 2)
                """);
        comando.executeUpdate("""
                INSERT INTO fontes (
                    id, tipo, origem_nome, url_origem, data_captura, leilao_id
                )
                VALUES
                    (1, 'LEILOEIRO_OFICIAL', 'Mega Leilões',
                     'https://www.megaleiloes.com.br/imoveis/teste-j1', CURRENT_TIMESTAMP, 1),
                    (2, 'LEILOEIRO_OFICIAL', 'Mega Leilões',
                     'https://www.megaleiloes.com.br/imoveis/teste-j2', CURRENT_TIMESTAMP, 2),
                    (3, 'LEILOEIRO_OFICIAL', 'Sublime Leilões',
                     'https://www.sublimeleiloes.com.br/lote/teste/3/', CURRENT_TIMESTAMP, 3)
                """);
        comando.executeUpdate("""
                INSERT INTO acompanhamentos (
                    id, imovel_id, status_pipeline, data_identificacao,
                    ultima_verificacao, ativo
                )
                VALUES
                    (1, 1, 'MONITORANDO_LEILAO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE),
                    (2, 2, 'MONITORANDO_LEILAO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE)
                """);
        comando.executeUpdate("""
                INSERT INTO historicos_acompanhamento (
                    id, acompanhamento_id, data_evento, status_pipeline, origem
                )
                VALUES
                    (1, 1, CURRENT_TIMESTAMP, 'MONITORANDO_LEILAO', 'Sublime Leilões'),
                    (2, 2, CURRENT_TIMESTAMP, 'MONITORANDO_LEILAO', 'Sublime Leilões')
                """);
    }

    private String consultarOrigem(
            Statement consulta,
            long historicoId
    ) throws Exception {

        try (ResultSet resultado =
                     consulta.executeQuery(
                             "SELECT origem FROM historicos_acompanhamento WHERE id = "
                                     + historicoId
                     )) {

            resultado.next();
            return resultado.getString(
                    "origem"
            );
        }
    }

    private boolean colunaExiste(
            Statement consulta,
            String tabela,
            String coluna
    ) throws Exception {

        String sql = """
                SELECT COUNT(*) AS total
                FROM information_schema.columns
                WHERE table_name = '%s'
                  AND column_name = '%s'
                """.formatted(tabela, coluna);

        try (ResultSet resultado =
                     consulta.executeQuery(sql)) {

            resultado.next();
            return resultado.getInt("total") == 1;
        }
    }
}
