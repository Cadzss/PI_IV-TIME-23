// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

/*
 * Resposta do servidor para a última operação pedida. O conteúdo é um texto JSON no formato
 * {"ok": true, "dados": ...} ou {"ok": false, "erro": "..."} (montado pelos métodos de Protocolo).
 */
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
        // String.valueOf evita NullPointerException se o valor vier null
        return String.valueOf(valorResultante);
    }
}
