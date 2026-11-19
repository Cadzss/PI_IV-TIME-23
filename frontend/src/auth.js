// Autor: Cadu Spadari e Arthur Zambroni
// Sessão simples no navegador (MVP). Troque as credenciais quando houver login no servidor.
// Cadastro interino: contas criadas na tela de cadastro ficam no localStorage deste navegador
// até existir cadastro no servidor.

const CHAVE = 'ew_sessao'
const CHAVE_USUARIOS = 'ew_usuarios'

export const FUNCOES = {
  coordenador: 'Coordenador',
  professor: 'Professor',
  aluno: 'Aluno',
}

const USUARIOS = [
  { email: 'coordenador@escola.local', senha: 'coordenador', nome: 'Coordenador', funcao: 'coordenador' },
]

function usuariosCadastrados() {
  try {
    const bruto = localStorage.getItem(CHAVE_USUARIOS)
    const lista = bruto ? JSON.parse(bruto) : []
    return Array.isArray(lista) ? lista : []
  } catch {
    return []
  }
}

function todosUsuarios() {
  return [...USUARIOS, ...usuariosCadastrados()]
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

export function entrar(email, senha) {
  const usuario = todosUsuarios().find(
    (u) => u.email.toLowerCase() === email.trim().toLowerCase() && u.senha === senha,
  )
  if (!usuario) throw new Error('E-mail ou senha incorretos.')
  const sessao = { email: usuario.email, nome: usuario.nome, funcao: usuario.funcao, em: Date.now() }
  localStorage.setItem(CHAVE, JSON.stringify(sessao))
  return sessao
}

// Cria a conta e já inicia a sessão. Lança Error com mensagem pronta para a tela.
export function cadastrar(dados) {
  const funcao = dados.funcao
  if (!FUNCOES[funcao]) throw new Error('Escolha uma função.')

  const nome = (dados.nome || '').trim()
  const email = (dados.email || '').trim().toLowerCase()
  const senha = dados.senha || ''

  if (nome.length < 3) throw new Error('Informe o nome completo.')
  if (!email) throw new Error('Informe o e-mail.')
  if (senha.length < 6) throw new Error('A senha precisa ter pelo menos 6 caracteres.')
  if (senha !== dados.confirmarSenha) throw new Error('As senhas não conferem.')

  const existentes = todosUsuarios()
  if (existentes.some((u) => u.email.toLowerCase() === email)) {
    throw new Error('Já existe uma conta com este e-mail.')
  }

  const usuario = { nome, email, senha, funcao }

  localStorage.setItem(CHAVE_USUARIOS, JSON.stringify([...usuariosCadastrados(), usuario]))
  return entrar(email, senha)
}

export function sair() {
  localStorage.removeItem(CHAVE)
}