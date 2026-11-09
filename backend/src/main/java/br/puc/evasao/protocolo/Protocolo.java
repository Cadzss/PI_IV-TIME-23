package br.puc.evasao.protocolo;

import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Protocolo de comunicação entre cliente e servidor via Socket.
 *
 * Cada mensagem é UMA linha de texto contendo um JSON.
 *
 * Pedido (cliente -> servidor):   {"acao": "LISTAR_ALUNOS", "dados": { ... }}
 * Resposta (servidor -> cliente): {"ok": true,  "dados": ...}
 *                                 {"ok": false, "erro": "mensagem"}
 *
 * OBS: este formato é provisório. Quando o padrão do professor for definido,
 * basta ajustar esta classe (e as classes que a usam) para segui-lo.
 */
public final class Protocolo {

    public static final String HOST_PADRAO = "localhost";
    public static final int PORTA_PADRAO = 12345;

    // Ações aceitas pelo servidor
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

    public static void enviar(PrintWriter saida, Document mensagem) {
        saida.println(mensagem.toJson());
        saida.flush();
    }

    /** Lê a próxima mensagem; retorna null se a conexão foi encerrada. */
    public static Document receber(BufferedReader entrada) throws IOException {
        String linha = entrada.readLine();
        if (linha == null) {
            return null;
        }
        return Document.parse(linha);
    }
}
