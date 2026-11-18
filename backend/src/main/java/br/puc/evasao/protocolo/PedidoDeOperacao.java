// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

/**
 * Pede ao servidor uma operação do sistema, como LISTAR_ALUNOS ou IMPORTAR_CSV.
 * Os dados da operação vão em JSON, por exemplo {"turma": "8ºA"}.
 */
public class PedidoDeOperacao extends Comunicado
{
    private String operacao;
    private String dados;

    public PedidoDeOperacao (String operacao, String dados)
    {
        this.operacao = operacao;
        this.dados    = dados;
    }

    public String getOperacao ()
    {
        return this.operacao;
    }

    public String getDados ()
    {
        return this.dados;
    }

    public String toString ()
    {
        return (this.operacao+" "+this.dados);
    }
}
