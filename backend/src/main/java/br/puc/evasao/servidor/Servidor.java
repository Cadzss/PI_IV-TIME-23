// Autor: Cadu Spadari
// Alterações: Lucas Canto — monta o ProcessadorPedidos com os repositórios novos (providências)
//             e espera a fila de e-mails terminar antes de desligar
package br.puc.evasao.servidor;

import java.util.ArrayList;

import br.puc.evasao.protocolo.ComunicadoDeDesligamento;
import br.puc.evasao.protocolo.Parceiro;

/**
 * Servidor principal: a AceitadoraDeConexao aceita os clientes e cada um é atendido
 * por uma SupervisoraDeConexao. Toda a regra de negócio e o acesso ao MongoDB ficam deste lado.
 *
 * Este main segue quase igual ao exemplo do professor (até o jeito de escrever as chaves):
 * sobe a thread que aceita conexões e fica lendo comandos do teclado até alguém digitar "desativar".
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

        // Lista dos clientes conectados (cada um é um Parceiro). É compartilhada entre as
        // threads, por isso todo acesso a ela é feito dentro de synchronized (usuarios)
        ArrayList<Parceiro> usuarios =
        new ArrayList<Parceiro> ();

        // Monta as peças da regra de negócio: conexão com o banco + repositórios + processador
        Banco banco = new Banco();
        UsuarioRepositorio usuarioRepositorio = new UsuarioRepositorio(banco);
        usuarioRepositorio.garantirContasDemo(); // cria coordenador/professor de demonstração se não existirem
        AlertaRepositorio alertaRepositorio = new AlertaRepositorio(banco);
        ProcessadorPedidos processador = new ProcessadorPedidos(
                new AlunoRepositorio(banco),
                alertaRepositorio,
                usuarioRepositorio,
                new ProvidenciaRepositorio(banco));

        AceitadoraDeConexao aceitadoraDeConexao=null;
        try
        {
            aceitadoraDeConexao =
            new AceitadoraDeConexao (porta, usuarios, processador);
            aceitadoraDeConexao.start(); // thread que fica no accept() esperando clientes
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
                // Avisa todos os clientes conectados que o servidor vai cair e fecha cada conexão
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

                alertaRepositorio.aguardarEmails(); // não perde e-mail que ainda estava na fila
                banco.fechar();
                System.out.println ("O servidor foi desativado!\n");
                System.exit(0);
            }
            else
                System.err.println ("Comando invalido!\n");
        }
    }
}
