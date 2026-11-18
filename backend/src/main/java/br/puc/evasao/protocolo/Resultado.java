// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

/** Resposta da última operação, em JSON: {"ok": true, "dados": ...} ou {"ok": false, "erro": "..."}. */
public class Resultado extends Comunicado
{
    private String valorResultante;

    public Resultado (String valorResultante)
    {
        this.valorResultante = valorResultante;
    }

    public String getValorResultante ()
    {
        return this.valorResultante;
    }

    public String toString ()
    {
        return (""+this.valorResultante);
    }
}
