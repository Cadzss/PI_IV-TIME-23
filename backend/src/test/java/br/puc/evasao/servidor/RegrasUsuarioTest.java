// Autor: Lucas Canto
package br.puc.evasao.servidor;

/*
 * Testes das regras de cadastro, troca de senha e senha temporária (sem banco).
 */

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegrasUsuarioTest {

    private static final String SENHA_INST = RegrasUsuario.senhaInstitucionalEsperada();

    @Test
    void normalizaEmailParaMinusculoSemEspacos() {
        assertEquals("ana@escola.com", RegrasUsuario.normalizarEmail("  Ana@Escola.COM "));
        assertEquals("", RegrasUsuario.normalizarEmail(null));
    }

    @Test
    void recusaFuncaoNomeEmailESenhaInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> RegrasUsuario.validar("Ana Souza", "ana@escola.com", "123456", "diretor"));
        assertThrows(IllegalArgumentException.class,
                () -> RegrasUsuario.validar("An", "ana@escola.com", "123456", "professor"));
        assertThrows(IllegalArgumentException.class,
                () -> RegrasUsuario.validar("Ana Souza", "ana-sem-arroba", "123456", "professor"));
        assertThrows(IllegalArgumentException.class,
                () -> RegrasUsuario.validar("Ana Souza", "ana@escola.com", "12345", "professor"));
    }

    @Test
    void professorPrecisaDaSenhaInstitucionalCorreta() {
        assertThrows(IllegalArgumentException.class, () -> RegrasUsuario.validarCadastroPublico(
                "Ana Souza", "ana@escola.com", "123456", "professor", null));
        assertThrows(IllegalArgumentException.class, () -> RegrasUsuario.validarCadastroPublico(
                "Ana Souza", "ana@escola.com", "123456", "professor", SENHA_INST + "x"));
        assertDoesNotThrow(() -> RegrasUsuario.validarCadastroPublico(
                "Ana Souza", "ana@escola.com", "123456", "professor", SENHA_INST));
    }

    @Test
    void trocaDeSenhaExigeAtualENovaValidaEDiferente() {
        assertThrows(IllegalArgumentException.class, () -> RegrasUsuario.validarTrocaDeSenha("", "novaSenha1"));
        assertThrows(IllegalArgumentException.class, () -> RegrasUsuario.validarTrocaDeSenha("antiga1", "123"));
        IllegalArgumentException igual = assertThrows(IllegalArgumentException.class,
                () -> RegrasUsuario.validarTrocaDeSenha("mesma123", "mesma123"));
        assertTrue(igual.getMessage().contains("diferente"));
        assertDoesNotThrow(() -> RegrasUsuario.validarTrocaDeSenha("antiga1", "novaSenha1"));
    }

    @Test
    void senhaTemporariaEFacilDeDitarEValida() {
        String a = RegrasUsuario.gerarSenhaTemporaria();
        String b = RegrasUsuario.gerarSenhaTemporaria();
        assertTrue(a.matches("[a-z2-9]{5}-[a-z2-9]{5}"), a);
        assertTrue(!a.matches(".*[01oli].*"), "sem caracteres que se confundem: " + a);
        assertTrue(!a.equals(b));
        assertDoesNotThrow(() -> RegrasUsuario.validarSenha(a));
        assertTrue(SenhaHash.confere(a, SenhaHash.gerar(a)));
    }

    @Test
    void naoExisteContaDeAlunoPoisEleEBeneficiarioNaoUsuario() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> RegrasUsuario.validarCadastroPublico(
                        "Bia Lima", "bia@escola.com", "123456", "aluno", SENHA_INST));
        assertTrue(erro.getMessage().contains("coordenador"));
    }
}
