///Autor: Arthur Zambroni
package br.puc.evasao.servidor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Regras de validação do cadastro de usuário. Não acessa o banco, então é fácil de testar. */
public final class RegrasUsuario {

    public static final String COORDENADOR = "coordenador";
    public static final String PROFESSOR = "professor";
    public static final String ALUNO = "aluno";

    /** Funções que existem no sistema. */
    public static final List<String> FUNCOES = Arrays.asList(COORDENADOR, PROFESSOR, ALUNO);

    /** Funções que exigem senha institucional no cadastro. */
    public static final List<String> FUNCOES_COM_SENHA_INSTITUCIONAL = Arrays.asList(COORDENADOR, PROFESSOR);

    /**
     * Senha institucional da escola (cadastro de coordenador/professor).
     * Pode sobrescrever com a variável de ambiente SENHA_INSTITUCIONAL.
     */
    public static final String SENHA_INSTITUCIONAL_PADRAO = "escola-2026";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private RegrasUsuario() {
    }

    /** E-mail sem espaços nas pontas e em minúsculas (é a chave única da conta). */
    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String senhaInstitucionalEsperada() {
        String env = System.getenv("SENHA_INSTITUCIONAL");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        return SENHA_INSTITUCIONAL_PADRAO;
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

    /**
     * Cadastro pela tela: aluno livre; coordenador/professor exigem senha institucional.
     */
    public static void validarCadastroPublico(String nome, String email, String senha, String funcao,
            String senhaInstitucional) {
        validar(nome, email, senha, funcao);
        if (FUNCOES_COM_SENHA_INSTITUCIONAL.contains(funcao)) {
            if (senhaInstitucional == null || senhaInstitucional.isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe a senha institucional para cadastrar coordenador ou professor.");
            }
            if (!senhaInstitucionalEsperada().equals(senhaInstitucional)) {
                throw new IllegalArgumentException("Senha institucional incorreta.");
            }
        }
    }
}
