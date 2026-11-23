///Autor: Arthur Zambroni
// Alterações: Lucas Canto — fim da conta de aluno (relatório 2.1.3), validações separadas de nome/senha,
//             troca de senha e geração de senha temporária
package br.puc.evasao.servidor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Regras de validação do cadastro de usuário. Não acessa o banco, então é fácil de testar. */
public final class RegrasUsuario {

    public static final String COORDENADOR = "coordenador";
    public static final String PROFESSOR = "professor";

    /**
     * Funções que existem no sistema. O aluno é beneficiário, não usuário (relatório, seção 2.1.3),
     * por isso não há conta de aluno.
     */
    public static final List<String> FUNCOES = Arrays.asList(COORDENADOR, PROFESSOR);

    /** Funções que exigem senha institucional no cadastro. */
    public static final List<String> FUNCOES_COM_SENHA_INSTITUCIONAL = Arrays.asList(COORDENADOR, PROFESSOR);

    /**
     * Senha institucional da escola (cadastro de coordenador/professor).
     * Pode sobrescrever com a variável de ambiente SENHA_INSTITUCIONAL.
     */
    public static final String SENHA_INSTITUCIONAL_PADRAO = "escola-2026";

    // Regex simples de e-mail: algo@algo.algo, sem espaços (não tenta validar 100% da norma)
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
            throw new IllegalArgumentException("Escolha se você é coordenador ou professor.");
        }
        validarNome(nome);
        String emailNormal = normalizarEmail(email);
        if (emailNormal.length() > 254 || !EMAIL.matcher(emailNormal).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        validarSenha(senha);
    }

    public static void validarNome(String nome) {
        if (nome == null || nome.trim().length() < 3) {
            throw new IllegalArgumentException("Informe o nome completo.");
        }
        if (nome.trim().length() > 100) {
            throw new IllegalArgumentException("O nome pode ter no máximo 100 caracteres.");
        }
    }

    public static void validarSenha(String senha) {
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres.");
        }
        if (senha.length() > 128) {
            throw new IllegalArgumentException("A senha pode ter no máximo 128 caracteres.");
        }
    }

    /** Troca de senha pela própria pessoa: a nova precisa ser válida e diferente da atual. */
    public static void validarTrocaDeSenha(String senhaAtual, String novaSenha) {
        if (senhaAtual == null || senhaAtual.isEmpty()) {
            throw new IllegalArgumentException("Informe a sua senha atual.");
        }
        validarSenha(novaSenha);
        if (novaSenha.equals(senhaAtual)) {
            throw new IllegalArgumentException("A nova senha precisa ser diferente da atual.");
        }
    }

    // Sem letras/números que se confundem (0/O, 1/l/I), para ditar a senha temporária sem erro.
    private static final String ALFABETO_TEMPORARIA = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final java.security.SecureRandom ALEATORIO = new java.security.SecureRandom();

    /** Senha temporária gerada quando o coordenador redefine a senha de alguém da equipe. */
    public static String gerarSenhaTemporaria() {
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i == 5) senha.append('-');
            senha.append(ALFABETO_TEMPORARIA.charAt(ALEATORIO.nextInt(ALFABETO_TEMPORARIA.length())));
        }
        return senha.toString();
    }

    /** Cadastro pela tela: coordenador e professor precisam do código da escola (senha institucional). */
    public static void validarCadastroPublico(String nome, String email, String senha, String funcao,
            String senhaInstitucional) {
        validar(nome, email, senha, funcao);
        if (FUNCOES_COM_SENHA_INSTITUCIONAL.contains(funcao)) {
            if (senhaInstitucional == null || senhaInstitucional.isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe o código da escola.");
            }
            if (!senhaInstitucionalEsperada().equals(senhaInstitucional)) {
                throw new IllegalArgumentException("Código da escola incorreto.");
            }
        }
    }
}
