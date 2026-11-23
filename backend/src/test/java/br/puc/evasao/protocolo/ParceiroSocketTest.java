// Autor: Lucas Canto
package br.puc.evasao.protocolo;

/*
 * Testa o Parceiro (padrão do professor) sobre um Socket de verdade na própria máquina:
 * troca de mensagens, filtro de segurança e a tratadora encerrando quando a conexão cai.
 */

import br.puc.evasao.ponte.TratadoraDeComunicadoDeDesligamento;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa o Parceiro sobre um Socket de verdade (sem servidor nem MongoDB). */
class ParceiroSocketTest {

    /** Abre um par de Parceiros conectados: [0] = lado servidor, [1] = lado cliente. */
    private static Parceiro[] parConectado(ServerSocket ss) throws Exception {
        Socket ladoCliente = new Socket(InetAddress.getLoopbackAddress(), ss.getLocalPort());
        Socket ladoServidor = ss.accept();

        ObjectOutputStream txServidor = new ObjectOutputStream(ladoServidor.getOutputStream());
        ObjectOutputStream txCliente = new ObjectOutputStream(ladoCliente.getOutputStream());
        ObjectInputStream rxServidor = new ObjectInputStream(ladoServidor.getInputStream());
        ObjectInputStream rxCliente = new ObjectInputStream(ladoCliente.getInputStream());
        rxServidor.setObjectInputFilter(ObjectInputFilter.Config.createFilter(Protocolo.FILTRO_DESSERIALIZACAO));
        rxCliente.setObjectInputFilter(ObjectInputFilter.Config.createFilter(Protocolo.FILTRO_DESSERIALIZACAO));

        return new Parceiro[] {
                new Parceiro(ladoServidor, rxServidor, txServidor),
                new Parceiro(ladoCliente, rxCliente, txCliente)};
    }

    @Test
    void pedidoEResultadoAtravessamOSocket() throws Exception {
        try (ServerSocket ss = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Parceiro[] par = parConectado(ss);
            par[1].receba(new PedidoDeOperacao(Protocolo.LISTAR_TURMAS, "{\"token\":\"x\"}"));

            Comunicado espiado = par[0].espie();
            assertTrue(espiado instanceof PedidoDeOperacao);
            PedidoDeOperacao recebido = (PedidoDeOperacao) par[0].envie();
            assertEquals(Protocolo.LISTAR_TURMAS, recebido.getOperacao());
            assertEquals("{\"token\":\"x\"}", recebido.getDados());

            par[0].receba(new Resultado("{\"ok\":true}"));
            assertEquals("{\"ok\":true}", ((Resultado) par[1].envie()).getValorResultante());
            par[0].adeus();
            par[1].adeus();
        }
    }

    @Test
    void filtroRecusaObjetoQueNaoEDoProtocolo() throws Exception {
        try (ServerSocket ss = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket cliente = new Socket(InetAddress.getLoopbackAddress(), ss.getLocalPort());
             Socket servidor = ss.accept()) {
            ObjectOutputStream tx = new ObjectOutputStream(cliente.getOutputStream());
            ObjectInputStream rx = new ObjectInputStream(servidor.getInputStream());
            rx.setObjectInputFilter(ObjectInputFilter.Config.createFilter(Protocolo.FILTRO_DESSERIALIZACAO));

            tx.writeObject(new ArrayList<String>());
            tx.flush();
            assertThrows(java.io.InvalidClassException.class, rx::readObject);
        }
    }

    @Test
    void tratadoraTerminaQuandoAConexaoCai() throws Exception {
        try (ServerSocket ss = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Parceiro[] par = parConectado(ss);
            TratadoraDeComunicadoDeDesligamento tratadora = new TratadoraDeComunicadoDeDesligamento(par[1]);
            tratadora.setDaemon(true);
            tratadora.start();

            par[0].adeus(); // o "servidor" cai
            tratadora.join(5000);
            assertFalse(tratadora.isAlive(), "a tratadora deveria encerrar em vez de ficar em loop");
        }
    }
}
