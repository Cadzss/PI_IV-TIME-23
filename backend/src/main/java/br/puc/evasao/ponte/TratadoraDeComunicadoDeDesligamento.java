// Autor: Cadu Spadari
// Alterações: Lucas Canto — a thread encerra quando a conexão cai (antes ficava em loop usando 100% da CPU)
package br.puc.evasao.ponte;

import br.puc.evasao.protocolo.ComunicadoDeDesligamento;
import br.puc.evasao.protocolo.Parceiro;

/**
 * Encerra o cliente quando o servidor avisa que vai desligar.
 *
 * É uma thread separada (padrão da aula) que fica o tempo todo "espiando" o próximo comunicado
 * do servidor. Se for um ComunicadoDeDesligamento, encerra o programa. Ela usa espie() e não
 * envie() justamente para não "roubar" o Resultado que a thread principal está esperando.
 */
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
            } catch (Exception erro) {
                // A conexão caiu: encerra a thread em vez de repetir o erro sem parar (100% de CPU).
                // O Cliente percebe que esta thread terminou e reconecta no próximo pedido.
                return;
            }
        }
    }
}
