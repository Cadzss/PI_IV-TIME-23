import { api } from './api.js'

const CHAVE = 'ew_sessao'

export const FUNCOES = {
  coordenador: 'Coordenador',
  professor: 'Professor',
  aluno: 'Aluno',
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

  if (!FUNCOES[funcao]) {
    throw new Error('Escolha uma função.')
  }

  const nome = (dados.nome || '').trim()
  const email = (dados.email || '').trim().toLowerCase()
  const senha = dados.senha || ''

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

  const usuario = await api.cadastrarUsuario({
    nome,
    email,
    senha,
    funcao,
  })

  return iniciarSessao(usuario)
}

export function sair() {
  localStorage.removeItem(CHAVE)
}