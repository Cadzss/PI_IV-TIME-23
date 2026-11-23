// Autor: Lucas Canto
// (também alterado por Cadu Spadari e Arthur Zambroni — cadastro/autenticação de usuários)
// (sessão/token, permissões, prévia da planilha, lançamentos, providências, equipe e conta: Lucas Canto)
package br.puc.evasao.servidor;

import org.bson.Document;

import java.util.List;
import java.util.Set;

import br.puc.evasao.protocolo.Protocolo;

/**
 * Recebe um pedido do protocolo, executa a ação correspondente e monta a resposta.
 * Antes de executar, confere o token da sessão e se a função do usuário pode fazer aquilo.
 */
public class ProcessadorPedidos {

    private final AlunoRepositorio alunos;
    private final AlertaRepositorio alertas;
    private final UsuarioRepositorio usuarios;
    private final ProvidenciaRepositorio providencias;
    private final ImportadorCsv importador;
    private final Sessoes sessoes = new Sessoes();
    private final RelatorioSemanal relatorioSemanal;

    public ProcessadorPedidos(AlunoRepositorio alunos, AlertaRepositorio alertas, UsuarioRepositorio usuarios,
                              ProvidenciaRepositorio providencias) {
        this.alunos = alunos;
        this.alertas = alertas;
        this.usuarios = usuarios;
        this.providencias = providencias;
        this.importador = new ImportadorCsv(alunos, alertas);
        this.relatorioSemanal = new RelatorioSemanal(alunos, alertas);
    }

    public Document processar(Document pedido) {
        String acao = pedido.getString("acao");
        Document dados = pedido.get("dados", Document.class);
        if (dados == null) dados = new Document();

        try {
            if (acao == null) {
                return Protocolo.erro("Pedido sem 'acao'");
            }

            // Ações que não exigem sessão (login, cadastro e sair)
            switch (acao) {
                case Protocolo.AUTENTICAR:
                    return Protocolo.sucesso(comToken(usuarios.autenticar(
                            dados.getString("email"), dados.getString("senha"))));

                case Protocolo.CADASTRAR_USUARIO:
                    return Protocolo.sucesso(comToken(cadastrar(dados)));

                case Protocolo.ENCERRAR_SESSAO:
                    sessoes.encerrar(dados.getString("token"));
                    return Protocolo.sucesso(null);

                default:
                    break;
            }

            // Daqui para baixo precisa estar logado: o token que veio do navegador tem que ser válido
            Document usuario = sessoes.usuario(dados.getString("token"));
            if (usuario == null) {
                return Protocolo.erro("Sua sessão expirou. Entre novamente.", Protocolo.SESSAO_EXPIRADA);
            }
            // E a função da pessoa (coordenador/professor) precisa poder fazer essa ação
            String funcao = usuario.getString("funcao");
            if (!Permissoes.pode(funcao, acao)) {
                return Protocolo.erro("Você não tem permissão para fazer isso.");
            }

            switch (acao) {
                case Protocolo.LISTAR_ALUNOS: {
                    List<Document> lista = alunos.listar(dados.getString("turma"), dados.getString("nivel"));
                    Set<String> emAcompanhamento = providencias.matriculasEmAcompanhamento();
                    for (Document a : lista) {
                        a.put("emAcompanhamento", emAcompanhamento.contains(a.getString("matricula")));
                    }
                    return Protocolo.sucesso(lista);
                }

                case Protocolo.DETALHAR_ALUNO: {
                    Document aluno = detalhe(dados.getString("matricula"));
                    return aluno == null ? Protocolo.erro("Não encontramos esse aluno.") : Protocolo.sucesso(aluno);
                }

                case Protocolo.REGISTRAR_PROVIDENCIA: {
                    String matricula = dados.getString("matricula");
                    String observacao = RegrasProvidencia.validarRegistro(
                            dados.getString("acao"), dados.getString("observacao"));
                    if (alunos.detalhar(matricula) == null) {
                        return Protocolo.erro("Não encontramos esse aluno.");
                    }
                    providencias.registrar(matricula, dados.getString("acao"), observacao, usuario);
                    return Protocolo.sucesso(detalhe(matricula));
                }

                case Protocolo.CONCLUIR_PROVIDENCIA: {
                    String resultado = RegrasProvidencia.validarConclusao(dados.getString("resultado"));
                    String matricula = providencias.concluir(dados.getString("id"), resultado, usuario);
                    return Protocolo.sucesso(detalhe(matricula));
                }

                case Protocolo.LISTAR_TURMAS:
                    return Protocolo.sucesso(alunos.turmas());

                case Protocolo.CONTAR_AVISOS_NAO_LIDOS:
                    relatorioSemanal.gerarSeNecessario();
                    return Protocolo.sucesso(new Document("naoLidos", alertas.contarNaoLidos()));

                case Protocolo.ATUALIZAR_PERFIL: {
                    Document atualizado = usuarios.atualizarNome(usuario.getString("email"), dados.getString("nome"));
                    sessoes.atualizarNome(usuario.getString("email"), atualizado.getString("nome"));
                    return Protocolo.sucesso(atualizado);
                }

                case Protocolo.ALTERAR_SENHA:
                    usuarios.alterarSenha(usuario.getString("email"),
                            dados.getString("senhaAtual"), dados.getString("novaSenha"));
                    // Outras sessões abertas com a senha antiga são encerradas; esta continua.
                    sessoes.encerrarDoUsuario(usuario.getString("email"), dados.getString("token"));
                    return Protocolo.sucesso(null);

                case Protocolo.LISTAR_EQUIPE: {
                    List<Document> equipe = usuarios.listarEquipe();
                    for (Document pessoa : equipe) {
                        pessoa.put("voce", pessoa.getString("email").equals(usuario.getString("email")));
                    }
                    return Protocolo.sucesso(equipe);
                }

                case Protocolo.REMOVER_USUARIO: {
                    String email = RegrasUsuario.normalizarEmail(dados.getString("email"));
                    usuarios.remover(email, usuario.getString("email"));
                    sessoes.encerrarDoUsuario(email, null);
                    return Protocolo.sucesso(null);
                }

                case Protocolo.REDEFINIR_SENHA: {
                    String email = RegrasUsuario.normalizarEmail(dados.getString("email"));
                    String temporaria = usuarios.redefinirSenha(email, usuario.getString("email"));
                    sessoes.encerrarDoUsuario(email, null);
                    return Protocolo.sucesso(new Document("senhaTemporaria", temporaria));
                }

                case Protocolo.LISTAR_ALERTAS:
                    relatorioSemanal.gerarSeNecessario();
                    return Protocolo.sucesso(alertas.listar());

                case Protocolo.MARCAR_ALERTA_LIDO:
                    return alertas.marcarLido(dados.getString("id"))
                            ? Protocolo.sucesso(null)
                            : Protocolo.erro("Não encontramos esse aviso.");

                case Protocolo.PREVISUALIZAR_CSV:
                    return Protocolo.sucesso(importador.previsualizar(dados.getString("conteudo")));

                case Protocolo.SALVAR_LANCAMENTO: {
                    String matricula = dados.getString("matricula");
                    Document novo = ImportadorCsv.montarRegistro(
                            texto(dados, "data"),
                            numero(dados, "frequencia", true),
                            numero(dados, "nota", true),
                            numero(dados, "atrasos", false),
                            numero(dados, "atividades", false));
                    // dataOriginal: ao editar e trocar a data, o lançamento antigo sai
                    String original = dados.getString("dataOriginal");
                    String remover = original != null && !original.equals(novo.getString("data")) ? original : null;
                    alunos.alterarHistorico(matricula, remover, novo, alertas);
                    return Protocolo.sucesso(detalhe(matricula));
                }

                case Protocolo.EXCLUIR_LANCAMENTO: {
                    String matricula = dados.getString("matricula");
                    alunos.alterarHistorico(matricula, texto(dados, "data"), null, alertas);
                    return Protocolo.sucesso(detalhe(matricula));
                }

                case Protocolo.REMOVER_ALUNO: {
                    String matricula = dados.getString("matricula");
                    if (!alunos.remover(matricula)) {
                        return Protocolo.erro("Não encontramos esse aluno.");
                    }
                    alertas.removerDoAluno(matricula);
                    providencias.removerDoAluno(matricula);
                    return Protocolo.sucesso(null);
                }

                case Protocolo.IMPORTAR_CSV:
                    return Protocolo.sucesso(importador.importar(dados.getString("conteudo")));

                case Protocolo.RECALCULAR_RISCOS:
                    return Protocolo.sucesso(new Document("recalculados", alunos.recalcularTodos(alertas)));

                default:
                    return Protocolo.erro("Ação desconhecida: " + acao);
            }
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException = erro "esperado" (validação); a mensagem já é para o usuário ler
            return Protocolo.erro(e.getMessage());
        } catch (RuntimeException e) {
            // Erro inesperado: detalhe técnico no log do servidor, mensagem simples para a tela
            e.printStackTrace();
            return Protocolo.erro("Algo deu errado. Tente novamente em instantes.");
        }
    }

    private Document cadastrar(Document dados) {
        return usuarios.cadastrar(
                dados.getString("nome"), dados.getString("email"),
                dados.getString("senha"), dados.getString("funcao"),
                dados.getString("senhaInstitucional"));
    }

    /** Aluno com histórico, risco e providências (o que a tela de detalhe mostra). */
    private Document detalhe(String matricula) {
        Document aluno = alunos.detalhar(matricula);
        if (aluno != null) {
            aluno.put("providencias", providencias.listar(matricula));
        }
        return aluno;
    }

    /** Nome do campo como aparece na tela. */
    private static String rotulo(String campo) {
        switch (campo) {
            case "frequencia": return "frequência";
            case "atividades": return "atividades entregues";
            default: return campo;
        }
    }

    private static String texto(Document dados, String campo) {
        Object v = dados.get(campo);
        if (v == null || v.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Preencha o campo " + rotulo(campo) + ".");
        }
        return v.toString().trim();
    }

    /** Número vindo da tela (número ou texto com vírgula). Opcional vazio devolve null. */
    private static Double numero(Document dados, String campo, boolean obrigatorio) {
        Object v = dados.get(campo);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v == null || v.toString().trim().isEmpty()) {
            if (obrigatorio) {
                throw new IllegalArgumentException("Preencha o campo " + rotulo(campo) + ".");
            }
            return null;
        }
        try {
            return Double.parseDouble(v.toString().trim().replace("%", "").replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O campo " + rotulo(campo) + " precisa ser um número.");
        }
    }

    private Document comToken(Document usuario) {
        return new Document(usuario).append("token", sessoes.abrir(usuario));
    }
}
