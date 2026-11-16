package br.puc.evasao.ponte;

import br.puc.evasao.protocolo.Comunicado;
import br.puc.evasao.protocolo.Parceiro;
import br.puc.evasao.protocolo.PedidoDeOperacao;
import br.puc.evasao.protocolo.PedidoDeResultado;
import br.puc.evasao.protocolo.PedidoParaSair;
import br.puc.evasao.protocolo.Resultado;
import org.bson.Document;

import java.io.*;
import java.net.*;

/**
 * Cliente Socket Java: abre uma conexão com o Servidor e a mantém aberta.
 * A PonteHttp usa este cliente para repassar os pedidos do navegador.
 */
public class Cliente
{
    public static final String HOST_PADRAO  = "localhost";
    public static final int    PORTA_PADRAO = 12345;

    private Parceiro servidor;

    public Cliente (String host, int porta) throws Exception
    {
        Socket conexao=null;
        try
        {
            conexao = new Socket (host, porta);
        }
        catch (Exception erro)
        {
            throw new Exception ("Indique o servidor e a porta corretos!");
        }

        ObjectOutputStream transmissor=null;
        try
        {
            transmissor =
            new ObjectOutputStream(
            conexao.getOutputStream());
        }
        catch (Exception erro)
        {
            throw new Exception ("Indique o servidor e a porta corretos!");
        }

        ObjectInputStream receptor=null;
        try
        {
            receptor =
            new ObjectInputStream(
            conexao.getInputStream());
        }
        catch (Exception erro)
        {
            throw new Exception ("Indique o servidor e a porta corretos!");
        }

        try
        {
            this.servidor =
            new Parceiro (conexao, receptor, transmissor);
        }
        catch (Exception erro)
        {
            throw new Exception ("Indique o servidor e a porta corretos!");
        }

        TratadoraDeComunicadoDeDesligamento tratadoraDeComunicadoDeDesligamento = null;
        try
        {
            tratadoraDeComunicadoDeDesligamento = new TratadoraDeComunicadoDeDesligamento (this.servidor);
        }
        catch (Exception erro)
        {} // sei que servidor foi instanciado

        tratadoraDeComunicadoDeDesligamento.start();
    }

    /** Pede a operação, pede o resultado e espera o Resultado chegar. */
    public synchronized Document peca (String operacao, Document dados) throws Exception
    {
        this.servidor.receba (new PedidoDeOperacao (operacao, dados==null ? "{}" : dados.toJson()));
        this.servidor.receba (new PedidoDeResultado ());

        Comunicado comunicado = null;
        do
        {
            comunicado = (Comunicado)this.servidor.espie ();
        }
        while (!(comunicado instanceof Resultado));
        Resultado resultado = (Resultado)this.servidor.envie ();

        return Document.parse (resultado.getValorResultante());
    }

    public void sair ()
    {
        try
        {
            this.servidor.receba (new PedidoParaSair ());
        }
        catch (Exception erro)
        {}
    }
}
