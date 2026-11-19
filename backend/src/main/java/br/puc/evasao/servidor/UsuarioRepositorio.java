/// Autor: Arthur Zambroni
package br.puc.evasao.servidor;

import java.time.Instant;

import org.bson.Document;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * Coleção "usuarios". Formato de cada documento:
 * { nome, email (minúsculo, único), senhaHash, funcao: "coordenador"|"professor"|"aluno", criadoEm }
 * O hash da senha nunca sai daqui: quem chama recebe só nome, e-mail e função.
 */
public class UsuarioRepositorio {

    private final MongoCollection<Document> usuarios;

    public UsuarioRepositorio(Banco banco) {
        this.usuarios = banco.getDatabase().getCollection("usuarios");
        usuarios.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
    }

    public Document cadastrar(String nome, String email, String senha, String funcao) {
        RegrasUsuario.validar(nome, email, senha, funcao);

        Document novo = new Document("nome", nome.trim())
                .append("email", RegrasUsuario.normalizarEmail(email))
                .append("senhaHash", SenhaHash.gerar(senha))
                .append("funcao", funcao)
                .append("criadoEm", Instant.now().toString());

        try {
            usuarios.insertOne(novo);
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new IllegalArgumentException("Já existe uma conta com este e-mail.");
            }
            throw e;
        }
        return publico(novo);
    }

    public Document autenticar(String email, String senha) {
        Document usuario = usuarios.find(Filters.eq("email", RegrasUsuario.normalizarEmail(email))).first();
        if (usuario == null || !SenhaHash.confere(senha, usuario.getString("senhaHash"))) {
            throw new IllegalArgumentException("E-mail ou senha incorretos.");
        }
        return publico(usuario);
    }

    /** Cria a conta de demonstração do coordenador só se ainda não existir nenhum usuário. */
    public void garantirCoordenadorDemo() {
        if (usuarios.countDocuments() == 0) {
            cadastrar("Coordenador", "coordenador@escola.local", "coordenador", "coordenador");
            System.out.println("[Servidor] Conta de demonstração criada: coordenador@escola.local");
        }
    }

    private static Document publico(Document usuario) {
        return new Document("nome", usuario.getString("nome"))
                .append("email", usuario.getString("email"))
                .append("funcao", usuario.getString("funcao"));
    }
}