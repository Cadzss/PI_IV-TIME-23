// Autor: Cadu Spadari
package br.puc.evasao.ponte;

import br.puc.evasao.protocolo.ComunicadoDeDesligamento;
import br.puc.evasao.protocolo.Parceiro;

/** Encerra o cliente quando o servidor avisa que vai desligar. */
public class TratadoraDeComunicadoDeDesligamento extends Thread {

    private final Parceiro servidor;

    public TratadoraDeComunicadoDeDesligamento(Parceiro servidor) throws Exception {
        if (servidor == null) {
            throw new Exception("Porta invalida");
        }
        this.servidor = servidor;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                if (servidor.espie() instanceof ComunicadoDeDesligamento) {
                    System.out.println("\nO servidor vai ser desligado agora;");
                    System.err.println("volte mais tarde!\n");
                    System.exit(0);
                }
            } catch (Exception ignored) {
            }
        }
    }
}