package br.puc.evasao.servidor;

import br.puc.evasao.protocolo.Protocolo;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Atende um cliente conectado: lê pedidos, processa e devolve respostas até a conexão fechar. */
public class AtendenteCliente implements Runnable {

    private final Socket conexao;
    private final ProcessadorPedidos processador;

    public AtendenteCliente(Socket conexao, ProcessadorPedidos processador) {
        this.conexao = conexao;
        this.processador = processador;
    }

    @Override
    public void run() {
        String cliente = conexao.getRemoteSocketAddress().toString();
        try (Socket s = conexao;
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(
                     new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {

            Document pedido;
            while ((pedido = Protocolo.receber(entrada)) != null) {
                System.out.println("[Servidor] " + cliente + " -> " + pedido.getString("acao"));
                Protocolo.enviar(saida, processador.processar(pedido));
            }
        } catch (IOException e) {
            System.out.println("[Servidor] Conexão com " + cliente + " encerrada: " + e.getMessage());
        }
    }
}
