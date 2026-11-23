// Autor: Cadu Spadari
package br.puc.evasao.ponte;

import br.puc.evasao.protocolo.Protocolo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bson.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Ponte HTTP entre o React e o Servidor (Socket).
 * POST /api/comando — corpo: {"acao": "...", "dados": {...}}
 */
public class PonteHttp {

    public static void main(String[] args) throws IOException {
        int portaHttp = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        Cliente cliente;
        try {
            cliente = new Cliente(Cliente.HOST_PADRAO, Cliente.PORTA_PADRAO);
        } catch (Exception e) {
            System.err.println("[Ponte] " + e.getMessage() + " (Servidor em "
                    + Cliente.HOST_PADRAO + ":" + Cliente.PORTA_PADRAO + ")");
            System.exit(1);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(cliente::sair));

        HttpServer http = HttpServer.create(new InetSocketAddress(portaHttp), 0);
        http.setExecutor(Executors.newFixedThreadPool(8));
        http.createContext("/api/comando", troca -> atender(troca, cliente));
        http.start();

        System.out.println("[Ponte] HTTP em http://localhost:" + portaHttp + "/api/comando -> Socket "
                + Cliente.HOST_PADRAO + ":" + Cliente.PORTA_PADRAO);
    }

    private static void atender(HttpExchange troca, Cliente cliente) throws IOException {
        troca.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        troca.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        troca.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

        String metodo = troca.getRequestMethod();
        if ("OPTIONS".equals(metodo)) {
            troca.sendResponseHeaders(204, -1);
            troca.close();
            return;
        }
        if (!"POST".equals(metodo)) {
            responder(troca, 405, Protocolo.erro("Use POST"));
            return;
        }

        Document requisicao;
        try {
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requisicao = Document.parse(corpo);
        } catch (RuntimeException e) {
            responder(troca, 400, Protocolo.erro("Requisição inválida: " + e.getMessage()));
            return;
        }

        try {
            String acao = requisicao.getString("acao");
            Document dados = requisicao.get("dados", Document.class);
            responder(troca, 200, cliente.peca(acao, dados));
        } catch (Exception e) {
            responder(troca, 502, Protocolo.erro("Servidor Socket indisponível: " + e.getMessage()));
        }
    }

    private static void responder(HttpExchange troca, int status, Document corpo) throws IOException {
        byte[] bytes = corpo.toJson().getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = troca.getResponseBody()) {
            out.write(bytes);
        }
    }
}