// Autor: Lucas Canto
package br.puc.evasao.servidor;

/*
 * Testes do hash de senha: confere a certa, recusa a errada e o salt muda o hash.
 */

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaHashTest {

    @Test
    void conferePelaSenhaCertaERecusaAErrada() {
        String guardado = SenhaHash.gerar("segredo123");
        assertTrue(guardado.startsWith("pbkdf2$"));
        assertTrue(SenhaHash.confere("segredo123", guardado));
        assertFalse(SenhaHash.confere("segredo124", guardado));
    }

    @Test
    void mesmaSenhaGeraHashesDiferentesPorCausaDoSalt() {
        assertNotEquals(SenhaHash.gerar("segredo123"), SenhaHash.gerar("segredo123"));
    }

    @Test
    void recusaFormatoInvalidoSemLancarErro() {
        assertFalse(SenhaHash.confere("x", null));
        assertFalse(SenhaHash.confere("x", "texto-qualquer"));
        assertFalse(SenhaHash.confere("x", "pbkdf2$abc$$"));
    }
}
