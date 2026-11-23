// Autor: Cadu Spadari
// Alterações: Lucas Canto — filtro de classes na desserialização (só aceita objetos do protocolo)
package br.puc.evasao.servidor;

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
import java.util.ArrayList;

/**
 * Atende um cliente: recebe pedidos, chama ProcessadorPedidos e devolve Resultado.
 *
 * Funciona como no exemplo do professor: guarda o resultado da última operação e só manda
 * quando o cliente pede (PedidoDeResultado). Cada cliente tem a sua supervisora, então o
 * "resultado" de um não se mistura com o de outro.
 */
public class SupervisoraDeConexao extends Thread {

    // Resultado da última operação deste cliente (começa com um erro, caso peçam antes de operar)
    private Document resultado = Protocolo.erro("Nenhuma operacao foi pedida");
    private Parceiro usuario;
    private final Socket conexao;
    private final ArrayList<Parceiro> usuarios;
    private final ProcessadorPedidos processador;

    public SupervisoraDeConexao(Socket conexao, ArrayList<Parceiro> usuarios, ProcessadorPedidos processador)
            throws Exception {
        if (conexao == null) {
            throw new Exception("Conexao ausente");
        }
        if (usuarios == null) {
            throw new Exception("Usuarios ausentes");
        }
        if (processador == null) {
            throw new Exception("Processador ausente");
        }
        this.conexao = conexao;
        this.usuarios = usuarios;
        this.processador = processador;
    }

    @Override
    public void run() {
        // Igual ao cliente: primeiro o ObjectOutputStream, depois o ObjectInputStream
        ObjectOutputStream transmissor;
        try {
            transmissor = new ObjectOutputStream(conexao.getOutputStream());
        } catch (Exception e) {
            return;
        }

        ObjectInputStream receptor;
        try {
            receptor = new ObjectInputStream(conexao.getInputStream());
            // Só desserializa classes do protocolo (ninguém consegue mandar um objeto malicioso)
            receptor.setObjectInputFilter(
                    ObjectInputFilter.Config.createFilter(Protocolo.FILTRO_DESSERIALIZACAO));
        } catch (Exception e) {
            fecharSilencioso(transmissor, null);
            return;
        }

        try {
            usuario = new Parceiro(conexao, receptor, transmissor);
        } catch (Exception e) {
            return;
        }

        try {
            // Entra na lista de conectados (usada para avisar todo mundo quando o servidor desliga)
            synchronized (usuarios) {
                usuarios.add(usuario);
            }

            // Loop principal: lê um comunicado por vez e reage conforme o tipo dele
            for (;;) {
                Comunicado comunicado = usuario.envie();
                if (comunicado == null) {
                    return;
                }
                if (comunicado instanceof PedidoDeOperacao) {
                    PedidoDeOperacao pedido = (PedidoDeOperacao) comunicado;
                    System.out.println("[Servidor] " + conexao.getRemoteSocketAddress()
                            + " -> " + pedido.getOperacao());

                    // Os dados chegam como texto JSON; vira Document e vai para a regra de negócio
                    String jsonDados = pedido.getDados();
                    Document dados = jsonDados == null ? new Document() : Document.parse(jsonDados);
                    resultado = processador.processar(Protocolo.pedido(pedido.getOperacao(), dados));
                } else if (comunicado instanceof PedidoDeResultado) {
                    // Devolve o resultado guardado da última operação
                    usuario.receba(new Resultado(resultado.toJson()));
                } else if (comunicado instanceof PedidoParaSair) {
                    synchronized (usuarios) {
                        usuarios.remove(usuario);
                    }
                    usuario.adeus();
                }
            }
        } catch (Exception e) {
            // Caiu aqui quando a conexão fecha (cliente saiu ou caiu): tira da lista e fecha tudo
            synchronized (usuarios) {
                if (usuario != null) {
                    usuarios.remove(usuario);
                }
            }
            fecharSilencioso(transmissor, receptor);
        }
    }

    private static void fecharSilencioso(ObjectOutputStream transmissor, ObjectInputStream receptor) {
        try {
            if (transmissor != null) {
                transmissor.close();
            }
            if (receptor != null) {
                receptor.close();
            }
        } catch (Exception ignored) {
        }
    }
}