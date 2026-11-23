// Autor: Cadu Spadari
package br.puc.evasao.protocolo;

/*
 * Mensagem que o servidor manda para todos os clientes quando alguém digita "desativar"
 * no console dele. O cliente (TratadoraDeComunicadoDeDesligamento) fica de olho nessa
 * mensagem e, quando ela chega, encerra o programa. Não precisa de dados: o tipo já diz tudo.
 */
public class ComunicadoDeDesligamento extends Comunicado {
}
