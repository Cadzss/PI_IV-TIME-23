/// Autor: Arthur Zambroni
package br.puc.evasao.servidor;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de senha com PBKDF2 (já vem no Java) e um salt aleatório para cada senha.
 * Formato guardado no banco: pbkdf2$iteracoes$salt$hash (salt e hash em Base64).
 */
public final class SenhaHash {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 120_000;
    private static final int BITS_DO_HASH = 256;
    private static final int BYTES_DO_SALT = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SenhaHash() {
    }

    public static String gerar(String senha) {
        byte[] salt = new byte[BYTES_DO_SALT];
        ALEATORIO.nextBytes(salt);
        byte[] hash = derivar(senha, salt, ITERACOES);
        return "pbkdf2$" + ITERACOES + "$" + codificar(salt) + "$" + codificar(hash);
    }

    public static boolean confere(String senha, String guardado) {
        if (senha == null || guardado == null) return false;
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2")) return false;
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes <= 0) return false;
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] obtido = derivar(senha, salt, iteracoes);
            return MessageDigest.isEqual(esperado, obtido);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(String senha, byte[] salt, int iteracoes) {
        PBEKeySpec especificacao = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, BITS_DO_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacao).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível neste Java", e);
        } finally {
            especificacao.clearPassword();
        }
    }

    private static String codificar(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}