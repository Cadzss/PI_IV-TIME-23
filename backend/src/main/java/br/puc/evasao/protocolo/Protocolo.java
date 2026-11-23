// Autor: Cadu Spadari
// Alterações: Lucas Canto — novas ações (prévia da planilha, lançamentos, providências, equipe, conta),
//             código SESSAO_EXPIRADA e filtro de segurança da desserialização
package br.puc.evasao.protocolo;

import org.bson.Document;

/**
 * Nomes das operações e formato JSON do Resultado: {"ok": true, "dados": ...} ou {"ok": false, "erro": "..."}.
 * Os pedidos (menos AUTENTICAR e CADASTRAR_USUARIO) levam o "token" da sessão dentro dos dados.
 *
 * Basicamente é o "dicionário" combinado entre cliente e servidor: os dois lados usam estas
 * constantes, então se alguém digitar o nome de uma ação errado o compilador já reclama.
 */
public final class Protocolo {

    // ---- Alunos e painel ----
    public static final String LISTAR_ALUNOS = "LISTAR_ALUNOS";
    public static final String DETALHAR_ALUNO = "DETALHAR_ALUNO";
    public static final String LISTAR_TURMAS = "LISTAR_TURMAS";
    // ---- Avisos (Tela 3) ----
    public static final String LISTAR_ALERTAS = "LISTAR_ALERTAS";
    public static final String MARCAR_ALERTA_LIDO = "MARCAR_ALERTA_LIDO";
    // ---- Planilha ----
    public static final String IMPORTAR_CSV = "IMPORTAR_CSV";
    public static final String RECALCULAR_RISCOS = "RECALCULAR_RISCOS";
    public static final String PREVISUALIZAR_CSV = "PREVISUALIZAR_CSV";
    // ---- Correções pela tela do aluno ----
    public static final String SALVAR_LANCAMENTO = "SALVAR_LANCAMENTO";
    public static final String EXCLUIR_LANCAMENTO = "EXCLUIR_LANCAMENTO";
    public static final String REMOVER_ALUNO = "REMOVER_ALUNO";
    // ---- Providências (ações recomendadas da Tela 2) ----
    public static final String REGISTRAR_PROVIDENCIA = "REGISTRAR_PROVIDENCIA";
    public static final String CONCLUIR_PROVIDENCIA = "CONCLUIR_PROVIDENCIA";
    // ---- Conta, equipe e avisos no menu ----
    public static final String CONTAR_AVISOS_NAO_LIDOS = "CONTAR_AVISOS_NAO_LIDOS";
    public static final String ATUALIZAR_PERFIL = "ATUALIZAR_PERFIL";
    public static final String ALTERAR_SENHA = "ALTERAR_SENHA";
    public static final String LISTAR_EQUIPE = "LISTAR_EQUIPE";
    public static final String REMOVER_USUARIO = "REMOVER_USUARIO";
    public static final String REDEFINIR_SENHA = "REDEFINIR_SENHA";
    // ---- Login (não precisam de token) ----
    public static final String CADASTRAR_USUARIO = "CADASTRAR_USUARIO";
    public static final String AUTENTICAR = "AUTENTICAR";
    public static final String ENCERRAR_SESSAO = "ENCERRAR_SESSAO";

    /** Código de erro que faz o navegador voltar para a tela de login. */
    public static final String SESSAO_EXPIRADA = "SESSAO_EXPIRADA";

    /**
     * Classes que podem chegar pelo Socket (ObjectInputStream). Qualquer outra é recusada,
     * para ninguém mandar um objeto Java malicioso para o servidor.
     *
     * Lendo o filtro: profundidade máxima 5; aceita as classes do pacote protocolo e String;
     * "!*" no final recusa todo o resto. (Desserializar objeto de qualquer classe é uma falha
     * de segurança famosa em Java, por isso o filtro.)
     */
    public static final String FILTRO_DESSERIALIZACAO =
            "maxdepth=5;br.puc.evasao.protocolo.*;java.lang.String;!*";

    // Construtor privado: ninguém instancia, a classe só tem coisas estáticas
    private Protocolo() {
    }

    /** Monta o pedido no formato que o ProcessadorPedidos entende. */
    public static Document pedido(String acao, Document dados) {
        return new Document("acao", acao).append("dados", dados == null ? new Document() : dados);
    }

    /** Resposta de sucesso: {"ok": true, "dados": ...} */
    public static Document sucesso(Object dados) {
        return new Document("ok", true).append("dados", dados);
    }

    /** Resposta de erro: {"ok": false, "erro": "mensagem para mostrar na tela"} */
    public static Document erro(String mensagem) {
        return new Document("ok", false).append("erro", mensagem);
    }

    /** Erro com código, para o navegador saber o que fazer (ex.: SESSAO_EXPIRADA volta para o login). */
    public static Document erro(String mensagem, String codigo) {
        return erro(mensagem).append("codigo", codigo);
    }
}
