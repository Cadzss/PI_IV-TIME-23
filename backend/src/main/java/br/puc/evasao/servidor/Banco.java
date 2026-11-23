// Autor: Lucas Canto
package br.puc.evasao.servidor;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Conexão com o MongoDB. URI e nome do banco podem ser trocados por variáveis de ambiente.
 *
 * Padrão: mongodb://localhost:27017 e banco "early_warning_evasao". Nos testes usamos
 * MONGO_URI/MONGO_DB apontando para outro banco, para não mexer nos dados de verdade.
 * O MongoClient já tem um pool de conexões dentro, então um só objeto serve para o servidor todo.
 */
public class Banco {

    private final MongoClient cliente;
    private final MongoDatabase database;

    public Banco() {
        String uri = System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017");
        String nome = System.getenv().getOrDefault("MONGO_DB", "early_warning_evasao");
        this.cliente = MongoClients.create(uri);
        this.database = cliente.getDatabase(nome);
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void fechar() {
        cliente.close();
    }
}
