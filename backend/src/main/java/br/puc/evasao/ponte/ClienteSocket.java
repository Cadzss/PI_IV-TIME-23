package br.puc.evasao.ponte;

import br.puc.evasao.protocolo.Protocolo;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Cliente Java que conversa com o Servidor via Socket usando o Protocolo. */
public class ClienteSocket {

    private final String host;
    private final int porta;

    public ClienteSocket(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    /** Abre a conexão, envia um pedido, aguarda a resposta e fecha a conexão. */
    public Document enviar(Document pedido) throws IOException {
        try (Socket socket = new Socket(host, porta);
             PrintWriter saida = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            Protocolo.enviar(saida, pedido);
            Document resposta = Protocolo.receber(entrada);
            if (resposta == null) {
                throw new IOException("Servidor encerrou a conexão sem responder");
            }
            return resposta;
        }
    }
}
