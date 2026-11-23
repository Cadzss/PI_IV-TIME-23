// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/*
 * Parceiro = "o outro lado da conversa". Segue o padrão da aula: guarda o Socket e os dois
 * streams de objetos (um para mandar e um para receber) e oferece quatro métodos:
 *
 *   receba(x) -> o parceiro RECEBE x, ou seja, eu mando x para ele
 *   envie()   -> o parceiro me ENVIA o próximo comunicado (eu leio e consumo)
 *   espie()   -> dou uma "espiada" no próximo comunicado SEM consumir
 *   adeus()   -> fecha tudo
 *
 * Os nomes parecem invertidos, mas é do ponto de vista do parceiro (é assim no material do professor).
 * Esta classe é usada dos dois lados: o servidor tem um Parceiro para cada cliente e o cliente
 * tem um Parceiro que representa o servidor.
 */
public class Parceiro {

    private final Socket conexao;
    private final ObjectInputStream receptor;
    private final ObjectOutputStream transmissor;

    // Semáforo com 1 vaga = exclusão mútua (mutex). Garante que duas threads não leiam
    // do receptor ao mesmo tempo dentro do espie() (o cliente tem a thread principal e a
    // TratadoraDeComunicadoDeDesligamento espiando o mesmo Parceiro).
    private final Semaphore mutEx = new Semaphore(1, true);

    // Guarda o comunicado que já foi lido mas ainda não foi consumido (por causa do espie)
    private Comunicado proximoComunicado;

    public Parceiro(Socket conexao, ObjectInputStream receptor, ObjectOutputStream transmissor) throws Exception {
        // Validações do padrão da aula: nada pode vir nulo
        if (conexao == null) {
            throw new Exception("Conexao ausente");
        }
        if (receptor == null) {
            throw new Exception("Receptor ausente");
        }
        if (transmissor == null) {
            throw new Exception("Transmissor ausente");
        }
        this.conexao = conexao;
        this.receptor = receptor;
        this.transmissor = transmissor;
    }

    /** Manda um comunicado para o parceiro. */
    public void receba(Comunicado x) throws Exception {
        try {
            transmissor.writeObject(x);
            // flush força o envio na hora (senão o objeto pode ficar parado no buffer)
            transmissor.flush();
        } catch (IOException e) {
            throw new Exception("Erro de transmissao");
        }
    }

    /** Olha qual é o próximo comunicado sem tirar ele da "fila". */
    public Comunicado espie() throws Exception {
        try {
            mutEx.acquireUninterruptibly();
            if (proximoComunicado == null) {
                // readObject bloqueia (fica esperando) até chegar alguma coisa pelo Socket
                proximoComunicado = (Comunicado) receptor.readObject();
            }
            mutEx.release();
            return proximoComunicado;
        } catch (Exception e) {
            throw new Exception("Erro de recepcao");
        }
    }

    /** Pega o próximo comunicado e consome (se alguém já espiou, devolve esse mesmo). */
    public Comunicado envie() throws Exception {
        try {
            if (proximoComunicado == null) {
                proximoComunicado = (Comunicado) receptor.readObject();
            }
            Comunicado ret = proximoComunicado;
            proximoComunicado = null; // consumiu: o próximo espie/envie vai ler um novo
            return ret;
        } catch (Exception e) {
            throw new Exception("Erro de recepcao");
        }
    }

    /** Fecha os streams e o Socket (fim da conversa). */
    public void adeus() throws Exception {
        try {
            transmissor.close();
            receptor.close();
            conexao.close();
        } catch (Exception e) {
            throw new Exception("Erro de desconexao");
        }
    }
}
