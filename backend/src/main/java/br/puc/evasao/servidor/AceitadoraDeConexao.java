// Autor: Cadu Spadari
package br.puc.evasao.servidor;

import br.puc.evasao.protocolo.Parceiro;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/** Thread que aceita conexões Socket e abre uma supervisora para cada cliente. */
public class AceitadoraDeConexao extends Thread {

    private final ServerSocket pedido;
    private final ArrayList<Parceiro> usuarios;
    private final ProcessadorPedidos processador;

    public AceitadoraDeConexao(String porta, ArrayList<Parceiro> usuarios, ProcessadorPedidos processador)
            throws Exception {
        if (porta == null) {
            throw new Exception("Porta ausente");
        }
        if (usuarios == null) {
            throw new Exception("Usuarios ausentes");
        }
        if (processador == null) {
            throw new Exception("Processador ausente");
        }

        try {
            this.pedido = new ServerSocket(Integer.parseInt(porta));
        } catch (Exception e) {
            throw new Exception("Porta invalida");
        }
        this.usuarios = usuarios;
        this.processador = processador;
    }

    @Override
    public void run() {
        for (;;) {
            Socket conexao;
            try {
                conexao = pedido.accept();
            } catch (Exception e) {
                continue;
            }

            SupervisoraDeConexao supervisora = null;
            try {
                supervisora = new SupervisoraDeConexao(conexao, usuarios, processador);
            } catch (Exception ignored) {
            }
            if (supervisora != null) {
                supervisora.start();
            }
        }
    }
}