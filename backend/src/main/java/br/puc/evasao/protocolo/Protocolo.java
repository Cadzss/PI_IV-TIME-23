package br.puc.evasao.protocolo;

import org.bson.Document;

/**
 * Operações aceitas pelo servidor e o formato do Resultado.
 *
 * Pelo Socket viajam objetos Comunicado (ObjectOutputStream / ObjectInputStream):
 *   cliente -> servidor: PedidoDeOperacao, depois PedidoDeResultado
 *   servidor -> cliente: Resultado
 *   PedidoParaSair encerra a conexão; ComunicadoDeDesligamento avisa que o servidor vai desligar.
 *
 * O valor do Resultado é um JSON: {"ok": true, "dados": ...} ou {"ok": false, "erro": "mensagem"}.
 */
public final class Protocolo {

    public static final String LISTAR_ALUNOS = "LISTAR_ALUNOS";
    public static final String DETALHAR_ALUNO = "DETALHAR_ALUNO";
    public static final String LISTAR_TURMAS = "LISTAR_TURMAS";
    public static final String LISTAR_ALERTAS = "LISTAR_ALERTAS";
    public static final String MARCAR_ALERTA_LIDO = "MARCAR_ALERTA_LIDO";
    public static final String IMPORTAR_CSV = "IMPORTAR_CSV";
    public static final String RECALCULAR_RISCOS = "RECALCULAR_RISCOS";

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
