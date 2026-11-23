// Autor: Lucas Canto
package br.puc.evasao.servidor;

import br.puc.evasao.protocolo.Protocolo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * O que cada função pode pedir ao servidor. Conferido no servidor em todo pedido.
 */
public final class Permissoes {

    // Mapa função -> conjunto de ações que ela pode pedir. Quem não está na lista não pode.
    // (É uma "lista de permitidos": ação nova só funciona depois de ser colocada aqui.)
    private static final Map<String, Set<String>> POR_FUNCAO = Map.of(
            RegrasUsuario.COORDENADOR, conjunto(
                    Protocolo.LISTAR_ALUNOS, Protocolo.DETALHAR_ALUNO, Protocolo.LISTAR_TURMAS,
                    Protocolo.LISTAR_ALERTAS, Protocolo.MARCAR_ALERTA_LIDO,
                    Protocolo.PREVISUALIZAR_CSV, Protocolo.IMPORTAR_CSV, Protocolo.RECALCULAR_RISCOS,
                    Protocolo.SALVAR_LANCAMENTO, Protocolo.EXCLUIR_LANCAMENTO, Protocolo.REMOVER_ALUNO,
                    Protocolo.REGISTRAR_PROVIDENCIA, Protocolo.CONCLUIR_PROVIDENCIA,
                    Protocolo.CONTAR_AVISOS_NAO_LIDOS, Protocolo.ATUALIZAR_PERFIL, Protocolo.ALTERAR_SENHA,
                    Protocolo.LISTAR_EQUIPE, Protocolo.REMOVER_USUARIO, Protocolo.REDEFINIR_SENHA),
            RegrasUsuario.PROFESSOR, conjunto(
                    Protocolo.LISTAR_ALUNOS, Protocolo.DETALHAR_ALUNO, Protocolo.LISTAR_TURMAS,
                    Protocolo.PREVISUALIZAR_CSV, Protocolo.IMPORTAR_CSV, Protocolo.RECALCULAR_RISCOS,
                    Protocolo.SALVAR_LANCAMENTO, Protocolo.EXCLUIR_LANCAMENTO,
                    Protocolo.REGISTRAR_PROVIDENCIA, Protocolo.CONCLUIR_PROVIDENCIA,
                    Protocolo.ATUALIZAR_PERFIL, Protocolo.ALTERAR_SENHA));

    private Permissoes() {
    }

    public static boolean pode(String funcao, String acao) {
        return funcao != null && POR_FUNCAO.getOrDefault(funcao, Collections.emptySet()).contains(acao);
    }

    private static Set<String> conjunto(String... acoes) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(acoes)));
    }
}
