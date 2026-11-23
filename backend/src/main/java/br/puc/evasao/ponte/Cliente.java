// Autor: Cadu Spadari
package br.puc.evasao.ponte;

import br.puc.evasao.protocolo.Comunicado;
import br.puc.evasao.protocolo.Parceiro;
import br.puc.evasao.protocolo.PedidoDeOperacao;
import br.puc.evasao.protocolo.PedidoDeResultado;
import br.puc.evasao.protocolo.PedidoParaSair;
import br.puc.evasao.protocolo.Resultado;
import org.bson.Document;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/** Cliente Socket usado pela PonteHttp para falar com o Servidor. */
public class Cliente {

    public static final String HOST_PADRAO = "localhost";
    public static final int PORTA_PADRAO = 12345;

    private static final String ERRO_CONEXAO = "Indique o servidor e a porta corretos!";

    private final Parceiro servidor;

    public Cliente(String host, int porta) throws Exception {
        Socket conexao;
        ObjectOutputStream transmissor;
        ObjectInputStream receptor;
        try {
            conexao = new Socket(host, porta);
            transmissor = new ObjectOutputStream(conexao.getOutputStream());
            receptor = new ObjectInputStream(conexao.getInputStream());
            this.servidor = new Parceiro(conexao, receptor, transmissor);
        } catch (Exception e) {
            throw new Exception(ERRO_CONEXAO);
        }

        TratadoraDeComunicadoDeDesligamento tratadora = new TratadoraDeComunicadoDeDesligamento(servidor);
        tratadora.start();
    }

    /** Envia operação + pedido de resultado e devolve o JSON da resposta. */
    public synchronized Document peca(String operacao, Document dados) throws Exception {
        String jsonDados = dados == null ? "{}" : dados.toJson();
        servidor.receba(new PedidoDeOperacao(operacao, jsonDados));
        servidor.receba(new PedidoDeResultado());

        Comunicado comunicado;
        do {
            comunicado = servidor.espie();
        } while (!(comunicado instanceof Resultado));

        Resultado resultado = (Resultado) servidor.envie();
        return Document.parse(resultado.getValorResultante());
    }

    public void sair() {
        try {
            servidor.receba(new PedidoParaSair());
        } catch (Exception ignored) {
        }
    }
}