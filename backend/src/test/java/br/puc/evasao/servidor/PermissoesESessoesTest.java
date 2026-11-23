// Autor: Lucas Canto
package br.puc.evasao.servidor;

/*
 * Testes de quem pode fazer o quê (Permissoes) e do controle de sessões/tokens (Sessoes).
 */

import br.puc.evasao.protocolo.Protocolo;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissoesESessoesTest {

    @Test
    void coordenadorPodeTudoQueOPainelUsa() {
        assertTrue(Permissoes.pode(RegrasUsuario.COORDENADOR, Protocolo.LISTAR_ALERTAS));
        assertTrue(Permissoes.pode(RegrasUsuario.COORDENADOR, Protocolo.IMPORTAR_CSV));
        assertTrue(Permissoes.pode(RegrasUsuario.COORDENADOR, Protocolo.LISTAR_ALUNOS));
    }

    @Test
    void corrigirLancamentoPodeAmbosMasRemoverAlunoSoCoordenador() {
        assertTrue(Permissoes.pode(RegrasUsuario.COORDENADOR, Protocolo.REMOVER_ALUNO));
        assertFalse(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.REMOVER_ALUNO));
        assertTrue(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.SALVAR_LANCAMENTO));
        assertTrue(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.EXCLUIR_LANCAMENTO));
        assertTrue(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.PREVISUALIZAR_CSV));
        assertTrue(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.REGISTRAR_PROVIDENCIA));
        assertTrue(Permissoes.pode(RegrasUsuario.COORDENADOR, Protocolo.CONCLUIR_PROVIDENCIA));
    }

    @Test
    void equipeEAvisosSoCoordenadorEPerfilAmbos() {
        assertTrue(Permissoes.pode(RegrasUsuario.COORDENADOR, Protocolo.LISTAR_EQUIPE));
        assertTrue(Permissoes.pode(RegrasUsuario.COORDENADOR, Protocolo.REDEFINIR_SENHA));
        assertTrue(Permissoes.pode(RegrasUsuario.COORDENADOR, Protocolo.CONTAR_AVISOS_NAO_LIDOS));
        assertFalse(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.LISTAR_EQUIPE));
        assertFalse(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.REMOVER_USUARIO));
        assertFalse(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.CONTAR_AVISOS_NAO_LIDOS));
        assertTrue(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.ALTERAR_SENHA));
        assertTrue(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.ATUALIZAR_PERFIL));
    }

    @Test
    void encerraAsSessoesDeUmaPessoaMenosAAtualEAtualizaONome() {
        Sessoes sessoes = new Sessoes();
        String celular = sessoes.abrir(new Document("email", "ana@e.com").append("nome", "Ana"));
        String notebook = sessoes.abrir(new Document("email", "ana@e.com").append("nome", "Ana"));
        String outra = sessoes.abrir(new Document("email", "bia@e.com").append("nome", "Bia"));

        sessoes.atualizarNome("ana@e.com", "Ana Souza");
        assertEquals("Ana Souza", sessoes.usuario(celular).getString("nome"));

        sessoes.encerrarDoUsuario("ana@e.com", notebook);
        assertNull(sessoes.usuario(celular));
        assertEquals("ana@e.com", sessoes.usuario(notebook).getString("email"));
        assertEquals("bia@e.com", sessoes.usuario(outra).getString("email"));

        sessoes.encerrarDoUsuario("ana@e.com", null);
        assertNull(sessoes.usuario(notebook));
    }

    @Test
    void professorNaoVeAlertas() {
        assertTrue(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.IMPORTAR_CSV));
        assertFalse(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.LISTAR_ALERTAS));
        assertFalse(Permissoes.pode(RegrasUsuario.PROFESSOR, Protocolo.MARCAR_ALERTA_LIDO));
    }

    @Test
    void funcaoInexistenteOuAntigaNaoPodeNada() {
        assertFalse(Permissoes.pode("aluno", Protocolo.DETALHAR_ALUNO));
        assertFalse(Permissoes.pode("aluno", Protocolo.LISTAR_ALUNOS));
        assertFalse(Permissoes.pode(null, Protocolo.LISTAR_ALUNOS));
        assertFalse(Permissoes.pode("diretor", Protocolo.LISTAR_ALUNOS));
    }

    @Test
    void sessaoDevolveOUsuarioDoTokenEEncerra() {
        Sessoes sessoes = new Sessoes();
        Document usuario = new Document("email", "a@b.com").append("funcao", RegrasUsuario.PROFESSOR);
        String token = sessoes.abrir(usuario);
        assertNotEquals(token, sessoes.abrir(usuario));
        assertEquals("a@b.com", sessoes.usuario(token).getString("email"));

        sessoes.encerrar(token);
        assertNull(sessoes.usuario(token));
        assertNull(sessoes.usuario("token-inventado"));
        assertNull(sessoes.usuario(null));
    }
}
