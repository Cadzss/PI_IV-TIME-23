package br.puc.evasao.servidor;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/** Conexão com o MongoDB. URI e nome do banco podem ser trocados por variáveis de ambiente. */
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
