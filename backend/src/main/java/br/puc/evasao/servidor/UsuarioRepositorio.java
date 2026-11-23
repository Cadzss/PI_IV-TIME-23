/// Autor: Arthur Zambroni
// Alterações: Lucas Canto — bloqueio de contas antigas de aluno, Minha conta (nome e senha),
//             Equipe (listar, remover acesso, redefinir senha com senha temporária)
package br.puc.evasao.servidor;

import java.time.Instant;

import org.bson.Document;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

import java.util.ArrayList;
import java.util.List;

/**
 * Coleção "usuarios". Formato de cada documento:
 * { nome, email (minúsculo, único), senhaHash, funcao: "coordenador"|"professor", criadoEm,
 *   senhaTemporaria? (true depois que o coordenador redefine a senha, até a pessoa trocar) }
 * O hash da senha nunca sai daqui: quem chama recebe só nome, e-mail e função.
 */
public class UsuarioRepositorio {

    private final MongoCollection<Document> usuarios;

    public UsuarioRepositorio(Banco banco) {
        this.usuarios = banco.getDatabase().getCollection("usuarios");
        // Índice único no e-mail: o próprio banco recusa duas contas com o mesmo e-mail
        usuarios.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
    }

    /** Cadastro pela tela pública (coordenador/professor com o código da escola). */
    public Document cadastrar(String nome, String email, String senha, String funcao, String senhaInstitucional) {
        RegrasUsuario.validarCadastroPublico(nome, email, senha, funcao, senhaInstitucional);
        return inserir(nome, email, senha, funcao);
    }

    public Document autenticar(String email, String senha) {
        Document usuario = usuarios.find(Filters.eq("email", RegrasUsuario.normalizarEmail(email))).first();
        // Mesma mensagem para e-mail inexistente e senha errada: não revela quais e-mails têm conta
        if (usuario == null || !SenhaHash.confere(senha, usuario.getString("senhaHash"))) {
            throw new IllegalArgumentException("E-mail ou senha incorretos.");
        }
        // Contas antigas de aluno (função que não existe mais) não entram.
        if (!RegrasUsuario.FUNCOES.contains(usuario.getString("funcao"))) {
            throw new IllegalArgumentException("Esta conta não tem acesso ao sistema. Fale com a coordenação da escola.");
        }
        Document resposta = publico(usuario);
        if (Boolean.TRUE.equals(usuario.getBoolean("senhaTemporaria"))) {
            resposta.append("senhaTemporaria", true);
        }
        return resposta;
    }

    /** Troca o nome mostrado no sistema. */
    public Document atualizarNome(String email, String nome) {
        RegrasUsuario.validarNome(nome);
        Document usuario = buscar(email);
        usuarios.updateOne(Filters.eq("_id", usuario.getObjectId("_id")), Updates.set("nome", nome.trim()));
        usuario.put("nome", nome.trim());
        return publico(usuario);
    }

    /** A própria pessoa troca a senha (precisa saber a atual). Tira a marca de senha temporária. */
    public void alterarSenha(String email, String senhaAtual, String novaSenha) {
        RegrasUsuario.validarTrocaDeSenha(senhaAtual, novaSenha);
        Document usuario = buscar(email);
        if (!SenhaHash.confere(senhaAtual, usuario.getString("senhaHash"))) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }
        usuarios.updateOne(Filters.eq("_id", usuario.getObjectId("_id")), Updates.combine(
                Updates.set("senhaHash", SenhaHash.gerar(novaSenha)),
                Updates.unset("senhaTemporaria")));
    }

    /** Equipe da escola (coordenadores e professores), em ordem de nome. */
    public List<Document> listarEquipe() {
        List<Document> lista = new ArrayList<>();
        for (Document u : usuarios.find(Filters.in("funcao", RegrasUsuario.FUNCOES)).sort(Sorts.ascending("nome"))) {
            Document item = publico(u).append("criadoEm", u.getString("criadoEm"));
            lista.add(item);
        }
        return lista;
    }

    /** O coordenador remove o acesso de alguém (não pode remover a si mesmo). */
    public void remover(String emailAlvo, String emailSolicitante) {
        String alvo = RegrasUsuario.normalizarEmail(emailAlvo);
        if (alvo.equals(RegrasUsuario.normalizarEmail(emailSolicitante))) {
            throw new IllegalArgumentException("Você não pode remover a sua própria conta.");
        }
        if (usuarios.deleteOne(Filters.eq("email", alvo)).getDeletedCount() == 0) {
            throw new IllegalArgumentException("Não encontramos essa pessoa.");
        }
    }

    /** O coordenador redefine a senha de alguém: devolve uma senha temporária para repassar à pessoa. */
    public String redefinirSenha(String emailAlvo, String emailSolicitante) {
        String alvo = RegrasUsuario.normalizarEmail(emailAlvo);
        if (alvo.equals(RegrasUsuario.normalizarEmail(emailSolicitante))) {
            throw new IllegalArgumentException("Para trocar a sua própria senha, use Minha conta.");
        }
        Document usuario = buscar(alvo);
        String temporaria = RegrasUsuario.gerarSenhaTemporaria();
        usuarios.updateOne(Filters.eq("_id", usuario.getObjectId("_id")), Updates.combine(
                Updates.set("senhaHash", SenhaHash.gerar(temporaria)),
                Updates.set("senhaTemporaria", true)));
        return temporaria;
    }

    private Document buscar(String email) {
        Document usuario = usuarios.find(Filters.eq("email", RegrasUsuario.normalizarEmail(email))).first();
        if (usuario == null) {
            throw new IllegalArgumentException("Não encontramos essa pessoa.");
        }
        return usuario;
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
            // DUPLICATE_KEY = o índice único do e-mail barrou (já existe conta com esse e-mail)
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new IllegalArgumentException("Já existe uma conta com esse e-mail.");
            }
            throw e;
        }
        return publico(novo);
    }

    /** O que pode sair do servidor sobre o usuário (nunca o hash da senha). */
    private static Document publico(Document usuario) {
        return new Document("nome", usuario.getString("nome"))
                .append("email", usuario.getString("email"))
                .append("funcao", usuario.getString("funcao"));
    }
}
