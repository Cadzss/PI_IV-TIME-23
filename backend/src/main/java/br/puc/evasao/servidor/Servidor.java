// Autor: Cadu Spadari
package br.puc.evasao.servidor;

import br.puc.evasao.protocolo.ComunicadoDeDesligamento;
import br.puc.evasao.protocolo.Parceiro;

import java.util.*;

/**
 * Servidor principal: a AceitadoraDeConexao aceita os clientes e cada um é atendido
 * por uma SupervisoraDeConexao. Toda a regra de negócio e o acesso ao MongoDB ficam deste lado.
 */
public class Servidor
{
    public static String PORTA_PADRAO = "12345";

    public static void main (String[] args)
    {
        if (args.length>1)
        {
            System.err.println ("Uso esperado: java Servidor [PORTA]\n");
            return;
        }

        String porta=Servidor.PORTA_PADRAO;

        if (args.length==1)
            porta = args[0];

        ArrayList<Parceiro> usuarios =
        new ArrayList<Parceiro> ();

        Banco banco = new Banco();
        ProcessadorPedidos processador = new ProcessadorPedidos(
                new AlunoRepositorio(banco),
                new AlertaRepositorio(banco));

        AceitadoraDeConexao aceitadoraDeConexao=null;
        try
        {
            aceitadoraDeConexao =
            new AceitadoraDeConexao (porta, usuarios, processador);
            aceitadoraDeConexao.start();
        }
        catch (Exception erro)
        {
            System.err.println ("Escolha uma porta apropriada e liberada para uso!\n");
            banco.fechar();
            return;
        }

        System.out.println ("[Servidor] Aguardando conexoes na porta "+porta+"...");

        for(;;)
        {
            System.out.println ("O servidor esta ativo! Para desativa-lo,");
            System.out.println ("use o comando \"desativar\"\n");
            System.out.print   ("> ");

            String comando=null;
            try
            {
                comando = Teclado.getUmString();
            }
            catch (Exception erro)
            {}

            if (comando==null)
            {
                // sem teclado (o rodar.sh sobe o servidor em segundo plano): segue atendendo ate ser encerrado
                try
                {
                    aceitadoraDeConexao.join();
                }
                catch (InterruptedException erro)
                {}
                return;
            }

            if (comando.toLowerCase().equals("desativar"))
            {
                synchronized (usuarios)
                {
                    ComunicadoDeDesligamento comunicadoDeDesligamento =
                    new ComunicadoDeDesligamento ();

                    for (Parceiro usuario:usuarios)
                    {
                        try
                        {
                            usuario.receba (comunicadoDeDesligamento);
                            usuario.adeus  ();
                        }
                        catch (Exception erro)
                        {}
                    }
                }

                banco.fechar();
                System.out.println ("O servidor foi desativado!\n");
                System.exit(0);
            }
            else
                System.err.println ("Comando invalido!\n");
        }
    }
}
