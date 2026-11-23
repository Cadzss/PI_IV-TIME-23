// Autor: Cadu Spadari
// Alterações: Lucas Canto — escuta só em localhost (sem CORS aberto), limite de 10 MB,
//             validação da requisição (400) e mensagens de erro sem termos técnicos
package br.puc.evasao.ponte;

import br.puc.evasao.protocolo.Protocolo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Ponte HTTP entre o React e o Servidor (Socket).
 * POST /api/comando — corpo: {"acao": "...", "dados": {...}}
 * Escuta só em localhost: o navegador chega aqui pelo proxy do Vite (mesma origem, sem CORS).
 *
 * Por que essa ponte existe? O navegador não consegue abrir um Socket TCP "cru" (só fala HTTP).
 * Então a ponte é um programinha Java que recebe HTTP do React e repassa o pedido para o Servidor
 * usando o Cliente Socket (no padrão do professor). Ela não tem regra de negócio nenhuma: só traduz.
 */
public class PonteHttp {

    /** Tamanho máximo do corpo da requisição (planilhas grandes cabem com folga). */
    private static final int LIMITE_CORPO = 10 * 1024 * 1024;

    public static void main(String[] args) throws IOException {
        // Porta HTTP pode vir como argumento; se não vier, usa 8080
        int portaHttp = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        // Abre a conexão Socket com o servidor logo no início; se o servidor não estiver no ar, encerra
        Cliente cliente;
        try {
            cliente = new Cliente(Cliente.HOST_PADRAO, Cliente.PORTA_PADRAO);
        } catch (Exception e) {
            System.err.println("[Ponte] " + e.getMessage() + " (Servidor em "
                    + Cliente.HOST_PADRAO + ":" + Cliente.PORTA_PADRAO + ")");
            System.exit(1);
            return;
        }
        // Quando a ponte for fechada (Ctrl+C), avisa o servidor que está saindo (PedidoParaSair)
        Runtime.getRuntime().addShutdownHook(new Thread(cliente::sair));

        // HttpServer já vem no Java (com.sun.net.httpserver), não precisou de biblioteca.
        // getLoopbackAddress = 127.0.0.1: ninguém de fora da máquina consegue acessar a ponte.
        HttpServer http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), portaHttp), 0);
        http.setExecutor(Executors.newFixedThreadPool(8)); // até 8 requisições HTTP em paralelo
        http.createContext("/api/comando", troca -> atender(troca, cliente));
        http.start();

        System.out.println("[Ponte] HTTP em http://localhost:" + portaHttp + "/api/comando -> Socket "
                + Cliente.HOST_PADRAO + ":" + Cliente.PORTA_PADRAO);
    }

    /** Trata uma requisição HTTP: valida o JSON, repassa pelo Socket e devolve a resposta. */
    private static void atender(HttpExchange troca, Cliente cliente) throws IOException {
        String metodo = troca.getRequestMethod();
        if (!"POST".equals(metodo)) {
            responder(troca, 405, Protocolo.erro("Use POST"));
            return;
        }

        String acao;
        Document dados;
        try {
            // Lê no máximo LIMITE_CORPO + 1 bytes: se passar do limite, sabemos que é grande demais
            byte[] bytes;
            try (InputStream entrada = troca.getRequestBody()) {
                bytes = entrada.readNBytes(LIMITE_CORPO + 1);
            }
            if (bytes.length > LIMITE_CORPO) {
                responder(troca, 413, Protocolo.erro("O arquivo é grande demais (máximo de 10 MB)."));
                return;
            }
            // Document.parse (do driver do MongoDB) transforma o texto JSON em um mapa
            Document requisicao = Document.parse(new String(bytes, StandardCharsets.UTF_8));
            Object valorAcao = requisicao.get("acao");
            Object valorDados = requisicao.get("dados");
            // Confere os tipos antes de usar, para devolver 400 (pedido errado) e não um erro genérico
            if (!(valorAcao instanceof String) || ((String) valorAcao).isEmpty()) {
                responder(troca, 400, Protocolo.erro("Requisição inválida: informe 'acao' como texto."));
                return;
            }
            if (valorDados != null && !(valorDados instanceof Document)) {
                responder(troca, 400, Protocolo.erro("Requisição inválida: 'dados' precisa ser um objeto."));
                return;
            }
            acao = (String) valorAcao;
            dados = (Document) valorDados;
        } catch (RuntimeException e) {
            responder(troca, 400, Protocolo.erro("Requisição inválida: JSON mal formado."));
            return;
        }

        try {
            // Aqui acontece a conversa pelo Socket (PedidoDeOperacao -> PedidoDeResultado -> Resultado)
            responder(troca, 200, cliente.peca(acao, dados));
        } catch (Exception e) {
            // O detalhe técnico vai para o log; para o usuário vai uma mensagem simples
            System.err.println("[Ponte] Servidor indisponível: " + e.getMessage());
            responder(troca, 502, Protocolo.erro("O sistema está fora do ar no momento. Tente novamente em instantes."));
        }
    }

    /** Escreve a resposta JSON com o status HTTP. */
    private static void responder(HttpExchange troca, int status, Document corpo) throws IOException {
        byte[] bytes = corpo.toJson().getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.sendResponseHeaders(status, bytes.length);
        // try-with-resources fecha o stream sozinho (e isso finaliza a resposta)
        try (OutputStream out = troca.getResponseBody()) {
            out.write(bytes);
        }
    }
}
