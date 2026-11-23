// Autor: Cadu Spadari
// Alterações: Lucas Canto — o ServerSocket só aceita conexões da própria máquina (localhost)
package br.puc.evasao.servidor;

import br.puc.evasao.protocolo.Parceiro;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Thread que aceita conexões Socket e abre uma supervisora para cada cliente.
 *
 * O accept() trava até alguém conectar; por isso ele roda numa thread própria e o main
 * do Servidor fica livre para ler o teclado. Cada cliente novo ganha uma SupervisoraDeConexao
 * (outra thread), então vários clientes podem ser atendidos ao mesmo tempo.
 */
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
            // Só aceita conexões da própria máquina (a PonteHttp roda junto com o servidor).
            // O 50 é o tamanho da fila de conexões esperando o accept.
            this.pedido = new ServerSocket(Integer.parseInt(porta), 50, InetAddress.getLoopbackAddress());
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
                conexao = pedido.accept(); // espera um cliente conectar
            } catch (Exception e) {
                continue;
            }

            SupervisoraDeConexao supervisora = null;
            try {
                supervisora = new SupervisoraDeConexao(conexao, usuarios, processador);
            } catch (Exception ignored) {
            }
            if (supervisora != null) {
                supervisora.start(); // cada cliente é atendido na sua própria thread
            }
        }
    }
}
