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

    /** Cadastro pela tela pública (aluno livre; coordenador/professor com senha institucional). */
    public Document cadastrar(String nome, String email, String senha, String funcao, String senhaInstitucional) {
        RegrasUsuario.validarCadastroPublico(nome, email, senha, funcao, senhaInstitucional);
        return inserir(nome, email, senha, funcao);
    }

    public Document autenticar(String email, String senha) {
        Document usuario = usuarios.find(Filters.eq("email", RegrasUsuario.normalizarEmail(email))).first();
        if (usuario == null || !SenhaHash.confere(senha, usuario.getString("senhaHash"))) {
            throw new IllegalArgumentException("E-mail ou senha incorretos.");
        }
        return publico(usuario);
    }

    /** Garante as contas de demonstração da banca. */
    public void garantirContasDemo() {
        garantirSeAusente("Coordenador", "coordenador@escola.local", "coordenador", RegrasUsuario.COORDENADOR);
        garantirSeAusente("Professor Demo", "professor@escola.local", "professor", RegrasUsuario.PROFESSOR);
    }

    private void garantirSeAusente(String nome, String email, String senha, String funcao) {
        String emailNormal = RegrasUsuario.normalizarEmail(email);
        if (usuarios.find(Filters.eq("email", emailNormal)).first() != null) {
            return;
        }
        inserir(nome, email, senha, funcao);
        System.out.println("[Servidor] Conta de demonstração criada: " + emailNormal + " (" + funcao + ")");
    }

    private Document inserir(String nome, String email, String senha, String funcao) {
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

    private static Document publico(Document usuario) {
        return new Document("nome", usuario.getString("nome"))
                .append("email", usuario.getString("email"))
                .append("funcao", usuario.getString("funcao"));
    }
}
