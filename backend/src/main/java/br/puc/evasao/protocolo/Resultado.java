// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

/** Resposta da última operação, em JSON. */
public class Resultado extends Comunicado {

    private final String valorResultante;

    public Resultado(String valorResultante) {
        this.valorResultante = valorResultante;
    }

    public String getValorResultante() {
        return valorResultante;
    }

    @Override
    public String toString() {
        return String.valueOf(valorResultante);
    }
}