// Autor: Lucas Canto
package br.puc.evasao.servidor;

/**
 * Regras das providências (ações recomendadas que alguém da escola assumiu).
 * Não acessa o banco, então é fácil de testar.
 */
public final class RegrasProvidencia {

    public static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String CONCLUIDA = "CONCLUIDA";

    static final int LIMITE_TEXTO = 500;

    private RegrasProvidencia() {
    }

    /** A ação precisa ser uma das ações recomendadas pelo sistema; a observação é opcional. */
    public static String validarRegistro(String acao, String observacao) {
        if (acao == null || !CalculadoraRisco.TODAS_AS_ACOES.contains(acao)) {
            throw new IllegalArgumentException("Escolha uma das ações recomendadas.");
        }
        return textoOpcional(observacao);
    }

    /** Para concluir é preciso contar o que aconteceu. */
    public static String validarConclusao(String resultado) {
        String texto = textoOpcional(resultado);
        if (texto == null || texto.length() < 3) {
            throw new IllegalArgumentException("Conte rapidamente o que aconteceu para concluir a providência.");
        }
        return texto;
    }

    private static String textoOpcional(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpo = texto.trim();
        if (limpo.length() > LIMITE_TEXTO) {
            throw new IllegalArgumentException("O texto pode ter no máximo " + LIMITE_TEXTO + " caracteres.");
        }
        return limpo;
    }
}
