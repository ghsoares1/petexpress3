package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class V4__completar_campos_de_usuario extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws SQLException {
        Set<String> existentes = new HashSet<>();
        try (var consulta = context.getConnection().createStatement();
             var colunas = consulta.executeQuery("PRAGMA table_info(usuario)")) {
            while (colunas.next()) {
                existentes.add(colunas.getString("name"));
            }
        }

        try (var alteracao = context.getConnection().createStatement()) {
            for (String coluna : List.of("sobrenome", "senha_hash", "endereco", "complemento", "bairro", "cep")) {
                if (!existentes.contains(coluna)) {
                    alteracao.executeUpdate("ALTER TABLE usuario ADD COLUMN " + coluna + " VARCHAR(255)");
                }
            }
            if (!existentes.contains("data_cadastro")) {
                alteracao.executeUpdate("ALTER TABLE usuario ADD COLUMN data_cadastro TIMESTAMP");
            }
        }
    }
}
