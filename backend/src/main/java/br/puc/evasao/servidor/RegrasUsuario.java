///Autor: Arthur Zambroni
package br.puc.evasao.servidor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Regras de validação do cadastro de usuário. Não acessa o banco, então é fácil de testar. */
public final class RegrasUsuario {

    public static final List<String> FUNCOES = Arrays.asList("coordenador", "professor", "aluno");

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private RegrasUsuario() {
    }

    /** E-mail sem espaços nas pontas e em minúsculas (é a chave única da conta). */
    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Lança IllegalArgumentException com a mensagem que a tela mostra para a pessoa. */
    public static void validar(String nome, String email, String senha, String funcao) {
        if (funcao == null || !FUNCOES.contains(funcao)) {
            throw new IllegalArgumentException("Escolha uma função válida.");
        }
        if (nome == null || nome.trim().length() < 3) {
            throw new IllegalArgumentException("Informe o nome completo.");
        }
        if (nome.trim().length() > 100) {
            throw new IllegalArgumentException("O nome pode ter no máximo 100 caracteres.");
        }
        String emailNormal = normalizarEmail(email);
        if (emailNormal.length() > 254 || !EMAIL.matcher(emailNormal).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres.");
        }
        if (senha.length() > 128) {
            throw new IllegalArgumentException("A senha pode ter no máximo 128 caracteres.");
        }
    }
}