import { api } from './api.js'

const CHAVE = 'ew_sessao'

export const FUNCOES = {
  coordenador: 'Coordenador',
  professor: 'Professor',
  aluno: 'Aluno',
}

/** Funções que pedem senha institucional no cadastro. */
export const FUNCOES_COM_SENHA_INSTITUCIONAL = ['coordenador', 'professor']

/** O que cada função pode abrir no painel. */
export const PERMISSOES = {
  coordenador: ['/', '/alertas', '/importar', '/alunos'],
  professor: ['/', '/importar', '/alunos'],
  aluno: ['/', '/alunos'],
}

export function sessaoAtual() {
  try {
    const bruto = localStorage.getItem(CHAVE)
    return bruto ? JSON.parse(bruto) : null
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

function iniciarSessao(usuario) {
  const sessao = {
    email: usuario.email,
    nome: usuario.nome,
    funcao: usuario.funcao,
    em: Date.now(),
  }

  localStorage.setItem(CHAVE, JSON.stringify(sessao))
  return sessao
}

export async function entrar(email, senha) {
  const usuario = await api.autenticar(email, senha)
  return iniciarSessao(usuario)
}

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
    throw new Error('As senhas não conferem.')
  }

  if (FUNCOES_COM_SENHA_INSTITUCIONAL.includes(funcao) && !senhaInstitucional) {
    throw new Error('Informe a senha institucional para cadastrar coordenador ou professor.')
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
  localStorage.removeItem(CHAVE)
}
