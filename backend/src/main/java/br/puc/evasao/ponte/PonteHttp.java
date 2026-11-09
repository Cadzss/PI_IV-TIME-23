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
 * Ponte entre o navegador e o servidor.
 *
 * O navegador (React) não consegue abrir um Socket TCP direto, então ele faz um POST HTTP
 * para esta ponte, que age como CLIENTE Socket Java e repassa o pedido ao Servidor.
 *
 *   React --HTTP--> PonteHttp (cliente Socket) --Socket--> Servidor --> MongoDB
 *
 * Endpoint único: POST /api/comando   corpo: {"acao": "...", "dados": {...}}
 */
public class PonteHttp {

    public static void main(String[] args) throws IOException {
        int portaHttp = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        ClienteSocket cliente = new ClienteSocket(Protocolo.HOST_PADRAO, Protocolo.PORTA_PADRAO);

        HttpServer http = HttpServer.create(new InetSocketAddress(portaHttp), 0);
        http.setExecutor(Executors.newFixedThreadPool(8));
        http.createContext("/api/comando", troca -> atender(troca, cliente));
        http.start();

        System.out.println("[Ponte] HTTP em http://localhost:" + portaHttp + "/api/comando -> Socket "
                + Protocolo.HOST_PADRAO + ":" + Protocolo.PORTA_PADRAO);
    }

    private static void atender(HttpExchange troca, ClienteSocket cliente) throws IOException {
        troca.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        troca.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        troca.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

        if ("OPTIONS".equals(troca.getRequestMethod())) {
            troca.sendResponseHeaders(204, -1);
            troca.close();
            return;
        }
        if (!"POST".equals(troca.getRequestMethod())) {
            responder(troca, 405, Protocolo.erro("Use POST"));
            return;
        }

        try {
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Document requisicao = Document.parse(corpo);
            Document pedido = Protocolo.pedido(requisicao.getString("acao"), requisicao.get("dados", Document.class));
            responder(troca, 200, cliente.enviar(pedido));
        } catch (IOException e) {
            responder(troca, 502, Protocolo.erro("Servidor Socket indisponível: " + e.getMessage()));
        } catch (RuntimeException e) {
            responder(troca, 400, Protocolo.erro("Requisição inválida: " + e.getMessage()));
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
