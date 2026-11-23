// Autor: Cadu Spadari
package br.puc.evasao.servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/*
 * Leitura do teclado, versão simplificada da classe Teclado usada nas aulas.
 * O Servidor usa para ler o comando "desativar" no console.
 */
public class Teclado {

    // Um único leitor para o programa todo (System.in não deve ser aberto várias vezes)
    private static final BufferedReader entrada =
            new BufferedReader(new InputStreamReader(System.in));

    /** Lê uma linha. Devolve null se não houver teclado (ex.: servidor rodando em segundo plano). */
    public static String getUmString() {
        try {
            return entrada.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
