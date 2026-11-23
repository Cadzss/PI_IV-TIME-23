// Autor: Cadu Spadari
package br.puc.evasao.servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Teclado {

    private static final BufferedReader entrada =
            new BufferedReader(new InputStreamReader(System.in));

    public static String getUmString() {
        try {
            return entrada.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}