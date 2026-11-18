// Autor: Cadu Spadari
package br.puc.evasao.servidor;

import br.puc.evasao.protocolo.Comunicado;
import br.puc.evasao.protocolo.Parceiro;
import br.puc.evasao.protocolo.PedidoDeOperacao;
import br.puc.evasao.protocolo.PedidoDeResultado;
import br.puc.evasao.protocolo.PedidoParaSair;
import br.puc.evasao.protocolo.Protocolo;
import br.puc.evasao.protocolo.Resultado;
import org.bson.Document;

import java.io.*;
import java.net.*;
import java.util.*;

public class SupervisoraDeConexao extends Thread
{
    private Document            resultado=Protocolo.erro("Nenhuma operacao foi pedida");
    private Parceiro            usuario;
    private Socket              conexao;
    private ArrayList<Parceiro> usuarios;
    private ProcessadorPedidos  processador;

    public SupervisoraDeConexao
    (Socket conexao, ArrayList<Parceiro> usuarios, ProcessadorPedidos processador)
    throws Exception
    {
        if (conexao==null)
            throw new Exception ("Conexao ausente");

        if (usuarios==null)
            throw new Exception ("Usuarios ausentes");

        if (processador==null)
            throw new Exception ("Processador ausente");

        this.conexao     = conexao;
        this.usuarios    = usuarios;
        this.processador = processador;
    }

    public void run ()
    {

        ObjectOutputStream transmissor;
        try
        {
            transmissor =
            new ObjectOutputStream(
            this.conexao.getOutputStream());
        }
        catch (Exception erro)
        {
            return;
        }

        ObjectInputStream receptor=null;
        try
        {
            receptor=
            new ObjectInputStream(
            this.conexao.getInputStream());
        }
        catch (Exception err0)
        {
            try
            {
                transmissor.close();
            }
            catch (Exception falha)
            {} // so tentando fechar antes de acabar a thread

            return;
        }

        try
        {
            this.usuario =
            new Parceiro (this.conexao,
                          receptor,
                          transmissor);
        }
        catch (Exception erro)
        {} // sei que passei os parametros corretos

        try
        {
            synchronized (this.usuarios)
            {
                this.usuarios.add (this.usuario);
            }


            for(;;)
            {
                Comunicado comunicado = this.usuario.envie ();

                if (comunicado==null)
                    return;
                else if (comunicado instanceof PedidoDeOperacao)
                {
                    PedidoDeOperacao pedidoDeOperacao = (PedidoDeOperacao)comunicado;

                    System.out.println ("[Servidor] "+this.conexao.getRemoteSocketAddress()+" -> "+pedidoDeOperacao.getOperacao());

                    Document dados = pedidoDeOperacao.getDados()==null
                                     ? new Document()
                                     : Document.parse (pedidoDeOperacao.getDados());

                    this.resultado = this.processador.processar (
                                     Protocolo.pedido (pedidoDeOperacao.getOperacao(), dados));
                }
                else if (comunicado instanceof PedidoDeResultado)
                {
                    this.usuario.receba (new Resultado (this.resultado.toJson()));
                }
                else if (comunicado instanceof PedidoParaSair)
                {
                    synchronized (this.usuarios)
                    {
                        this.usuarios.remove (this.usuario);
                    }
                    this.usuario.adeus();
                }
            }
        }
        catch (Exception erro)
        {
            synchronized (this.usuarios)
            {
                this.usuarios.remove (this.usuario);
            }

            try
            {
                transmissor.close ();
                receptor   .close ();
            }
            catch (Exception falha)
            {} // so tentando fechar antes de acabar a thread

            return;
        }
    }
}
