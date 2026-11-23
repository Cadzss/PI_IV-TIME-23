// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

import org.bson.Document;

/**
 * Nomes das operações e formato JSON do Resultado: {"ok": true, "dados": ...} ou {"ok": false, "erro": "..."}.
 */
public final class Protocolo {

    public static final String LISTAR_ALUNOS = "LISTAR_ALUNOS";
    public static final String DETALHAR_ALUNO = "DETALHAR_ALUNO";
    public static final String LISTAR_TURMAS = "LISTAR_TURMAS";
    public static final String LISTAR_ALERTAS = "LISTAR_ALERTAS";
    public static final String MARCAR_ALERTA_LIDO = "MARCAR_ALERTA_LIDO";
    public static final String IMPORTAR_CSV = "IMPORTAR_CSV";
    public static final String RECALCULAR_RISCOS = "RECALCULAR_RISCOS";
    public static final String CADASTRAR_USUARIO = "CADASTRAR_USUARIO";
    public static final String AUTENTICAR = "AUTENTICAR";

    private Protocolo() {
    }

    public static Document pedido(String acao, Document dados) {
        return new Document("acao", acao).append("dados", dados == null ? new Document() : dados);
    }

    public static Document sucesso(Object dados) {
        return new Document("ok", true).append("dados", dados);
    }

    public static Document erro(String mensagem) {
        return new Document("ok", false).append("erro", mensagem);
    }
}