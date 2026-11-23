// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

/** Pedido de operação (ex.: LISTAR_ALUNOS) com dados em JSON. */
public class PedidoDeOperacao extends Comunicado {

    private final String operacao;
    private final String dados;

    public PedidoDeOperacao(String operacao, String dados) {
        this.operacao = operacao;
        this.dados = dados;
    }

    public String getOperacao() {
        return operacao;
    }

    public String getDados() {
        return dados;
    }

    @Override
    public String toString() {
        return operacao + " " + dados;
    }
}