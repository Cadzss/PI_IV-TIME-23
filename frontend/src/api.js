// Autor: Cadu Spadari
// Alterações: Lucas Canto — envia o token da sessão em todo pedido, volta ao login quando a sessão expira,
//             mensagens de erro em linguagem simples e as chamadas novas (prévia, lançamentos, providências,
//             equipe, conta)
// React envia POST /api/comando → PonteHttp (Java) → Socket → Servidor → MongoDB
//
// Este arquivo é a ÚNICA porta de saída do React para o servidor: todas as telas chamam api.alguma_coisa().
// Assim, se o jeito de falar com o servidor mudar, muda só aqui.

export const CHAVE_SESSAO = 'ew_sessao'

function tokenAtual() {
  try {
    return JSON.parse(localStorage.getItem(CHAVE_SESSAO))?.token
  } catch {
    return undefined
  }
}

/**
 * Manda uma ação para o servidor e devolve os "dados" da resposta.
 * Se o servidor responder {ok: false}, lança um Error com a mensagem — as telas mostram essa mensagem.
 */
export async function enviar(acao, dados = {}) {
  let resposta
  try {
    resposta = await fetch('/api/comando', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      // O servidor confere o token em todo pedido para saber quem está pedindo.
      body: JSON.stringify({ acao, dados: { ...dados, token: tokenAtual() } }),
    })
  } catch {
    throw new Error('Não foi possível conectar. Verifique sua internet e tente novamente.')
  }

  let json = null
  try {
    json = await resposta.json()
  } catch {
    json = null // corpo vazio ou que não é JSON: tratado logo abaixo
  }

  if (!json) {
    throw new Error('O sistema está fora do ar no momento. Tente novamente em instantes.')
  }
  if (!json.ok) {
    if (json.codigo === 'SESSAO_EXPIRADA') {
      // Token vencido ou servidor reiniciado: volta para o login.
      localStorage.removeItem(CHAVE_SESSAO)
      window.location.assign('/login')
    }
    throw new Error(json.erro || 'Algo deu errado. Tente novamente.')
  }
  return json.dados
}

// Atalhos com nome amigável para cada ação do protocolo (os nomes em MAIÚSCULO são os do Protocolo.java)
export const api = {
  listarAlunos: (filtros) => enviar('LISTAR_ALUNOS', filtros),
  detalharAluno: (matricula) => enviar('DETALHAR_ALUNO', { matricula }),
  listarTurmas: () => enviar('LISTAR_TURMAS'),
  listarAlertas: () => enviar('LISTAR_ALERTAS'),
  marcarAlertaLido: (id) => enviar('MARCAR_ALERTA_LIDO', { id }),
  previsualizarCsv: (conteudo) => enviar('PREVISUALIZAR_CSV', { conteudo }),
  importarCsv: (conteudo) => enviar('IMPORTAR_CSV', { conteudo }),
  salvarLancamento: (matricula, lancamento, dataOriginal) =>
    enviar('SALVAR_LANCAMENTO', { matricula, ...lancamento, dataOriginal }),
  excluirLancamento: (matricula, data) => enviar('EXCLUIR_LANCAMENTO', { matricula, data }),
  removerAluno: (matricula) => enviar('REMOVER_ALUNO', { matricula }),
  registrarProvidencia: (matricula, acao, observacao) =>
    enviar('REGISTRAR_PROVIDENCIA', { matricula, acao, observacao }),
  concluirProvidencia: (id, resultado) => enviar('CONCLUIR_PROVIDENCIA', { id, resultado }),
  contarAvisosNaoLidos: () => enviar('CONTAR_AVISOS_NAO_LIDOS'),
  atualizarPerfil: (nome) => enviar('ATUALIZAR_PERFIL', { nome }),
  alterarSenha: (senhaAtual, novaSenha) => enviar('ALTERAR_SENHA', { senhaAtual, novaSenha }),
  listarEquipe: () => enviar('LISTAR_EQUIPE'),
  removerUsuario: (email) => enviar('REMOVER_USUARIO', { email }),
  redefinirSenha: (email) => enviar('REDEFINIR_SENHA', { email }),
  recalcularRiscos: () => enviar('RECALCULAR_RISCOS'),
  cadastrarUsuario: (dados) => enviar('CADASTRAR_USUARIO', dados),
  autenticar: (email, senha) => enviar('AUTENTICAR', { email, senha }),
  encerrarSessao: () => enviar('ENCERRAR_SESSAO'),
}

// O driver do Mongo serializa datas como {"$date": ...}
export function lerData(valor) {
  if (!valor) return null
  return new Date(valor.$date ?? valor)
}
