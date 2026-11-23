// Autor: Cadu Spadari
// Alterações: Lucas Canto — reconexão automática quando a conexão cai e filtro de classes
//             na desserialização (só aceita objetos do protocolo)
package br.puc.evasao.ponte;

import br.puc.evasao.protocolo.Comunicado;
import br.puc.evasao.protocolo.Parceiro;
import br.puc.evasao.protocolo.PedidoDeOperacao;
import br.puc.evasao.protocolo.PedidoDeResultado;
import br.puc.evasao.protocolo.PedidoParaSair;
import br.puc.evasao.protocolo.Protocolo;
import br.puc.evasao.protocolo.Resultado;
import org.bson.Document;

import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Cliente Socket usado pela PonteHttp para falar com o Servidor.
 * Se a conexão cair (ex.: o servidor reiniciou), o próximo pedido abre uma conexão nova.
 *
 * É o "lado cliente" do padrão da aula: abre o Socket, cria os streams de objetos, embrulha
 * tudo num Parceiro e liga a thread que vigia o comunicado de desligamento.
 */
public class Cliente {

    // 127.0.0.1 (e não "localhost") para não depender de como o sistema resolve o nome
    public static final String HOST_PADRAO = "127.0.0.1";
    public static final int PORTA_PADRAO = 12345;

    private static final String ERRO_CONEXAO = "Indique o servidor e a porta corretos!";

    private final String host;
    private final int porta;

    // Não são final porque são recriados quando a conexão cai e a gente reconecta
    private Parceiro servidor;
    private TratadoraDeComunicadoDeDesligamento tratadora;

    public Cliente(String host, int porta) throws Exception {
        this.host = host;
        this.porta = porta;
        conectar();
    }

    private void conectar() throws Exception {
        Socket conexao;
        ObjectOutputStream transmissor;
        ObjectInputStream receptor;
        try {
            conexao = new Socket(host, porta);
            // Ordem importa: cria o ObjectOutputStream ANTES do ObjectInputStream (dos dois lados),
            // senão os dois ficam esperando o cabeçalho um do outro e trava (deadlock)
            transmissor = new ObjectOutputStream(conexao.getOutputStream());
            receptor = new ObjectInputStream(conexao.getInputStream());
            // Só aceita receber classes do protocolo (segurança)
            receptor.setObjectInputFilter(
                    ObjectInputFilter.Config.createFilter(Protocolo.FILTRO_DESSERIALIZACAO));
            this.servidor = new Parceiro(conexao, receptor, transmissor);
        } catch (Exception e) {
            throw new Exception(ERRO_CONEXAO);
        }

        tratadora = new TratadoraDeComunicadoDeDesligamento(servidor);
        tratadora.setDaemon(true); // daemon: não impede o programa de fechar
        tratadora.start();
    }

    /**
     * Envia operação + pedido de resultado e devolve o JSON da resposta.
     * É synchronized porque a ponte atende vários HTTP em paralelo, mas a conexão Socket é uma só:
     * um pedido precisa terminar antes do próximo começar, senão as respostas se misturam.
     */
    public synchronized Document peca(String operacao, Document dados) throws Exception {
        // A tratadora termina quando a conexão cai; nesse caso, reconecta antes de pedir.
        if (!tratadora.isAlive()) {
            conectar();
        }

        try {
            String jsonDados = dados == null ? "{}" : dados.toJson();
            servidor.receba(new PedidoDeOperacao(operacao, jsonDados));
            servidor.receba(new PedidoDeResultado());

            // Espera chegar um Resultado (outros comunicados são deixados para a tratadora)
            Comunicado comunicado;
            do {
                comunicado = servidor.espie();
            } while (!(comunicado instanceof Resultado));

            Resultado resultado = (Resultado) servidor.envie();
            return Document.parse(resultado.getValorResultante());
        } catch (Exception e) {
            // Conexão em estado desconhecido: fecha para a tratadora encerrar e o próximo pedido reconectar.
            try {
                servidor.adeus();
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    /** Avisa o servidor que este cliente está indo embora. */
    public synchronized void sair() {
        try {
            servidor.receba(new PedidoParaSair());
        } catch (Exception ignored) {
        }
    }
}
