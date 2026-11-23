// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

import java.io.Serializable;

/*
 * Comunicado é a "classe mãe" de tudo que viaja pelo Socket, seguindo o padrão que o
 * professor passou em aula (o exemplo do Fazedor de Continhas).
 *
 * Ela precisa implementar Serializable porque o ObjectOutputStream só consegue mandar
 * pela rede objetos serializáveis (ele transforma o objeto em bytes e o outro lado
 * reconstrói o objeto a partir desses bytes).
 *
 * Não tem atributo nenhum: cada tipo de mensagem é uma subclasse (PedidoDeOperacao,
 * Resultado, PedidoParaSair...). Assim quem recebe usa "instanceof" para saber o que chegou.
 */
public class Comunicado implements Serializable, Cloneable {
}
