// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

/*
 * O cliente manda esta mensagem quando vai embora, para o servidor tirar ele da lista
 * de usuários conectados e fechar a conexão do jeito certo (sem dar erro de leitura).
 */
public class PedidoParaSair extends Comunicado {
}
