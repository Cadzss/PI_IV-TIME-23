import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { cadastrar, estaLogado, FUNCOES } from '../auth.js'

const campo =
  'rounded border border-slate-300 px-3 py-2 text-sm font-normal text-slate-800 outline-none focus:border-marinho'
const rotulo = 'grid gap-1 text-xs font-semibold text-slate-600'

const DESCRICOES = {
  coordenador: 'Acompanha a visão geral dos alunos em risco.',
  professor: 'Lança notas e frequência dos alunos.',
  aluno: 'Acompanha as próprias notas e frequência.',
}

const VAZIO = {
  nome: '',
  email: '',
  senha: '',
  confirmarSenha: '',
}

export default function Cadastro({ onEntrou }) {
  const navigate = useNavigate()
  const [funcao, setFuncao] = useState('')
  const [dados, setDados] = useState(VAZIO)
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  if (estaLogado()) return <Navigate to="/" replace />

  function atualizar(nome) {
    return (e) => setDados((d) => ({ ...d, [nome]: e.target.value }))
  }

  function enviar(e) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      cadastrar({ ...dados, funcao })
      onEntrou?.()
      navigate('/', { replace: true })
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-papel px-4 py-8">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <p className="text-sm font-semibold uppercase tracking-wide text-destaque">Early-Warning</p>
          <h1 className="mt-1 text-3xl font-bold text-marinho">Criar conta</h1>
          <p className="mt-2 text-sm text-slate-500">Escolha sua função e preencha seus dados.</p>
        </div>

        <form onSubmit={enviar} className="space-y-4 rounded-lg bg-white p-6 shadow-sm">
          <fieldset>
            <legend className="mb-2 text-xs font-semibold text-slate-600">Função</legend>
            <div className="grid grid-cols-3 gap-2" role="radiogroup">
              {Object.entries(FUNCOES).map(([chave, nome]) => (
                <label
                  key={chave}
                  className={`cursor-pointer rounded border px-3 py-2 text-center text-sm font-semibold focus-within:ring-2 focus-within:ring-marinho ${
                    funcao === chave
                      ? 'border-marinho bg-marinho text-white'
                      : 'border-slate-300 bg-white text-slate-700 hover:border-marinho'
                  }`}
                >
                  <input
                    type="radio"
                    name="funcao"
                    value={chave}
                    checked={funcao === chave}
                    onChange={() => setFuncao(chave)}
                    className="sr-only"
                  />
                  {nome}
                </label>
              ))}
            </div>
            <p className="mt-2 text-xs text-slate-500">
              {funcao ? DESCRICOES[funcao] : 'Escolha uma função para continuar.'}
            </p>
          </fieldset>

          {funcao && (
            <>
              <label className={rotulo}>
                Nome completo
                <input
                  type="text"
                  autoComplete="name"
                  required
                  value={dados.nome}
                  onChange={atualizar('nome')}
                  className={campo}
                />
              </label>

              <label className={rotulo}>
                E-mail
                <input
                  type="email"
                  autoComplete="username"
                  required
                  value={dados.email}
                  onChange={atualizar('email')}
                  className={campo}
                />
              </label>

              <label className={rotulo}>
                Senha
                <input
                  type="password"
                  autoComplete="new-password"
                  required
                  minLength={6}
                  value={dados.senha}
                  onChange={atualizar('senha')}
                  className={campo}
                  placeholder="Mínimo de 6 caracteres"
                />
              </label>

              <label className={rotulo}>
                Confirmar senha
                <input
                  type="password"
                  autoComplete="new-password"
                  required
                  value={dados.confirmarSenha}
                  onChange={atualizar('confirmarSenha')}
                  className={campo}
                />
              </label>
            </>
          )}

          {erro && (
            <p className="rounded border border-red-200 bg-risco-alerta px-3 py-2 text-sm text-risco-alto">
              {erro}
            </p>
          )}

          <button
            type="submit"
            disabled={enviando || !funcao}
            className="w-full rounded bg-marinho px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-40"
          >
            {enviando ? 'Criando conta…' : 'Criar conta'}
          </button>
        </form>

        <p className="mt-4 text-center text-xs text-slate-500">
          Já tem conta?{' '}
          <Link to="/login" className="font-medium text-marinho underline">
            Entrar
          </Link>
        </p>
      </div>
    </div>
  )
}
