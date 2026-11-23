// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

/*
 * Pedido de operação: o cliente diz QUAL ação quer (ex.: "LISTAR_ALUNOS") e manda os dados
 * da ação em texto JSON (ex.: {"turma": "8ºA", "token": "..."}).
 *
 * Por que os dados vão como String JSON e não como objeto? Porque assim a mesma classe serve
 * para todas as ações, e só passam pelo Socket tipos simples (String), o que também deixa o
 * filtro de segurança da desserialização bem restrito (ver Protocolo.FILTRO_DESSERIALIZACAO).
 */
public class PedidoDeOperacao extends Comunicado {

    // "final": depois de criado o pedido não muda (é só uma mensagem)
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

    // Só para aparecer bonitinho no log / depuração
    @Override
    public String toString() {
        return operacao + " " + dados;
    }
}
