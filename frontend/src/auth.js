// Autor: Cadu Spadari (também alterado por Arthur Zambroni)
// Alterações: Lucas Canto — sessão com token do servidor, fim da conta de aluno, rotas novas
//             (equipe e conta), aviso de senha temporária e atualização da sessão sem recarregar a página
//
// Controle de login no navegador. IMPORTANTE: aqui só decidimos o que MOSTRAR; quem garante a
// segurança de verdade é o servidor, que confere o token e a função em todo pedido.
import { api, CHAVE_SESSAO } from './api.js'

// O aluno é beneficiário, não usuário do sistema (relatório, seção 2.1.3).
export const FUNCOES = {
  coordenador: 'Coordenador',
  professor: 'Professor',
}

/** Funções que pedem senha institucional no cadastro. */
export const FUNCOES_COM_SENHA_INSTITUCIONAL = ['coordenador', 'professor']

/**
 * O que cada função pode abrir no painel. Isto só esconde telas: quem garante o acesso
 * de verdade é o servidor, que confere o token e a função em todo pedido.
 */
export const PERMISSOES = {
  coordenador: ['/', '/alertas', '/importar', '/alunos', '/equipe', '/conta'],
  professor: ['/', '/importar', '/alunos', '/conta'],
}

/** Lê a sessão guardada no localStorage (ou null se não estiver logado). */
export function sessaoAtual() {
  try {
    const bruto = localStorage.getItem(CHAVE_SESSAO)
    const sessao = bruto ? JSON.parse(bruto) : null
    return sessao?.token && FUNCOES[sessao.funcao] ? sessao : null
  } catch {
    return null
  }
}

export function estaLogado() {
  return Boolean(sessaoAtual())
}

export function podeAcessar(caminho) {
  const sessao = sessaoAtual()
  if (!sessao) return false
  const permitidos = PERMISSOES[sessao.funcao] || []
  if (caminho.startsWith('/alunos/')) return permitidos.includes('/alunos')
  return permitidos.includes(caminho)
}

/** Para onde mandar a pessoa depois do login (ou quando ela abre algo que não pode). */
export function paginaInicial() {
  return '/'
}

/** Guarda no navegador o que o servidor devolveu no login (inclusive o token). */
function iniciarSessao(usuario) {
  const sessao = {
    email: usuario.email,
    nome: usuario.nome,
    funcao: usuario.funcao,
    token: usuario.token,
    senhaTemporaria: Boolean(usuario.senhaTemporaria),
    em: Date.now(),
  }

  localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao))
  return sessao
}

/** Atualiza dados da sessão guardada (ex.: nome trocado em Minha conta) e avisa a tela. */
export function atualizarSessao(campos) {
  const sessao = sessaoAtual()
  if (!sessao) return
  localStorage.setItem(CHAVE_SESSAO, JSON.stringify({ ...sessao, ...campos }))
  window.dispatchEvent(new Event('sessao-atualizada'))
}

export async function entrar(email, senha) {
  const usuario = await api.autenticar(email, senha)
  return iniciarSessao(usuario)
}

/**
 * Cadastro: as validações daqui são só para avisar rápido na tela; o servidor valida tudo de novo
 * (nunca dá para confiar só no que vem do navegador).
 */
export async function cadastrar(dados) {
  const funcao = dados.funcao
  const nome = (dados.nome || '').trim()
  const email = (dados.email || '').trim().toLowerCase()
  const senha = dados.senha || ''
  const senhaInstitucional = dados.senhaInstitucional || ''

  if (!FUNCOES[funcao]) {
    throw new Error('Escolha uma função.')
  }

  if (nome.length < 3) {
    throw new Error('Informe o nome completo.')
  }

  if (!email) {
    throw new Error('Informe o e-mail.')
  }

  if (senha.length < 6) {
    throw new Error('A senha precisa ter pelo menos 6 caracteres.')
  }

  if (senha !== dados.confirmarSenha) {
    throw new Error('As duas senhas não são iguais.')
  }

  if (FUNCOES_COM_SENHA_INSTITUCIONAL.includes(funcao) && !senhaInstitucional) {
    throw new Error('Informe o código da escola.')
  }

  const usuario = await api.cadastrarUsuario({
    nome,
    email,
    senha,
    funcao,
    senhaInstitucional: FUNCOES_COM_SENHA_INSTITUCIONAL.includes(funcao) ? senhaInstitucional : undefined,
  })

  return iniciarSessao(usuario)
}

export function sair() {
  // Avisa o servidor para invalidar o token; se falhar, o token expira sozinho.
  api.encerrarSessao().catch(() => {})
  localStorage.removeItem(CHAVE_SESSAO)
}
