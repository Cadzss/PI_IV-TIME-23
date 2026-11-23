// Autor: Lucas Canto
package br.puc.evasao.servidor;

/*
 * Testes das regras das providências (ação válida, resultado obrigatório, limite de texto).
 */

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegrasProvidenciaTest {

    @Test
    void aceitaAsAcoesRecomendadasPeloSistema() {
        assertTrue(CalculadoraRisco.TODAS_AS_ACOES.contains("Acionar assistente social"));
        assertNull(RegrasProvidencia.validarRegistro("Acionar assistente social", "   "));
        assertEquals("Aluno relatou problema de transporte",
                RegrasProvidencia.validarRegistro("Contatar a família com urgência", "  Aluno relatou problema de transporte "));
    }

    @Test
    void recusaAcaoQueNaoFoiRecomendada() {
        assertThrows(IllegalArgumentException.class, () -> RegrasProvidencia.validarRegistro("Expulsar o aluno", null));
        assertThrows(IllegalArgumentException.class, () -> RegrasProvidencia.validarRegistro(null, null));
    }

    @Test
    void concluirExigeContarOResultado() {
        assertThrows(IllegalArgumentException.class, () -> RegrasProvidencia.validarConclusao(null));
        assertThrows(IllegalArgumentException.class, () -> RegrasProvidencia.validarConclusao(" ok"));
        assertEquals("Família contatada; aluno voltou a frequentar",
                RegrasProvidencia.validarConclusao("Família contatada; aluno voltou a frequentar"));
    }

    @Test
    void limitaOTamanhoDoTexto() {
        String longo = "x".repeat(RegrasProvidencia.LIMITE_TEXTO + 1);
        assertThrows(IllegalArgumentException.class,
                () -> RegrasProvidencia.validarRegistro("Acionar assistente social", longo));
    }
}
