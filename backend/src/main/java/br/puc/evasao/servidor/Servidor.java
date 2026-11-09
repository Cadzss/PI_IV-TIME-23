package br.puc.evasao.servidor;

import br.puc.evasao.protocolo.Protocolo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor principal: aceita conexões via Socket e atende cada cliente em uma thread separada.
 * Toda a regra de negócio e o acesso ao MongoDB ficam deste lado.
 */
public class Servidor {

    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;

        Banco banco = new Banco();
        ProcessadorPedidos processador = new ProcessadorPedidos(
                new AlunoRepositorio(banco),
                new AlertaRepositorio(banco));

        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("[Servidor] Aguardando conexões na porta " + porta + "...");
            while (true) {
                Socket conexao = servidor.accept();
                Thread atendente = new Thread(new AtendenteCliente(conexao, processador));
                atendente.start();
            }
        } finally {
            banco.fechar();
        }
    }
}
