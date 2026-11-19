// Autor: Cadu Spadari
// Sessão simples no navegador (MVP). Troque as credenciais quando houver login no servidor.

const CHAVE = 'ew_sessao'

const USUARIOS = [
  { email: 'coordenador@escola.local', senha: 'coordenador', nome: 'Coordenador' },
]

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
  const usuario = USUARIOS.find(
    (u) => u.email.toLowerCase() === email.trim().toLowerCase() && u.senha === senha,
  )
  if (!usuario) throw new Error('E-mail ou senha incorretos.')
  const sessao = { email: usuario.email, nome: usuario.nome, em: Date.now() }
  localStorage.setItem(CHAVE, JSON.stringify(sessao))
  return sessao
}

export function sair() {
  localStorage.removeItem(CHAVE)
}
