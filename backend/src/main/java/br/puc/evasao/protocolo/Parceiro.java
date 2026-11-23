// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/** Canal de ObjectStream sobre um Socket (cliente ou servidor). */
public class Parceiro {

    private final Socket conexao;
    private final ObjectInputStream receptor;
    private final ObjectOutputStream transmissor;
    private final Semaphore mutEx = new Semaphore(1, true);

    private Comunicado proximoComunicado;

    public Parceiro(Socket conexao, ObjectInputStream receptor, ObjectOutputStream transmissor) throws Exception {
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

    public void receba(Comunicado x) throws Exception {
        try {
            transmissor.writeObject(x);
            transmissor.flush();
        } catch (IOException e) {
            throw new Exception("Erro de transmissao");
        }
    }

    public Comunicado espie() throws Exception {
        try {
            mutEx.acquireUninterruptibly();
            if (proximoComunicado == null) {
                proximoComunicado = (Comunicado) receptor.readObject();
            }
            mutEx.release();
            return proximoComunicado;
        } catch (Exception e) {
            throw new Exception("Erro de recepcao");
        }
    }

    public Comunicado envie() throws Exception {
        try {
            if (proximoComunicado == null) {
                proximoComunicado = (Comunicado) receptor.readObject();
            }
            Comunicado ret = proximoComunicado;
            proximoComunicado = null;
            return ret;
        } catch (Exception e) {
            throw new Exception("Erro de recepcao");
        }
    }

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