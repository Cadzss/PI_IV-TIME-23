// Autor: Cadu Spadari
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { entrar, estaLogado } from '../auth.js'

export default function Login({ onEntrou }) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  if (estaLogado()) return <Navigate to="/" replace />

  async function enviar(e) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)

    try {
      await entrar(email, senha)
      onEntrou?.()
      navigate('/', { replace: true })
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-papel px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <p className="text-sm font-semibold uppercase tracking-wide text-destaque">Early-Warning</p>
          <h1 className="mt-1 text-3xl font-bold text-marinho">Evasão Escolar</h1>
          <p className="mt-2 text-sm text-slate-500">Entre para acompanhar os alunos em risco.</p>
        </div>

        <form onSubmit={enviar} className="space-y-4 rounded-lg bg-white p-6 shadow-sm">
          <label className="grid gap-1 text-xs font-semibold text-slate-600">
            E-mail
            <input
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="rounded border border-slate-300 px-3 py-2 text-sm font-normal text-slate-800 outline-none focus:border-marinho"
              placeholder="coordenador@escola.local"
            />
          </label>

          <label className="grid gap-1 text-xs font-semibold text-slate-600">
            Senha
            <input
              type="password"
              autoComplete="current-password"
              required
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              className="rounded border border-slate-300 px-3 py-2 text-sm font-normal text-slate-800 outline-none focus:border-marinho"
              placeholder="••••••••"
            />
          </label>

          {erro && (
            <p className="rounded border border-red-200 bg-risco-alerta px-3 py-2 text-sm text-risco-alto">
              {erro}
            </p>
          )}

          <button
            type="submit"
            disabled={enviando}
            className="w-full rounded bg-marinho px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-40"
          >
            {enviando ? 'Entrando…' : 'Entrar'}
          </button>
        </form>

        <p className="mt-4 text-center text-xs text-slate-500">
          Não tem conta?{' '}
          <Link to="/cadastro" className="font-medium text-marinho underline">
            Criar conta
          </Link>
        </p>

        <p className="mt-4 text-center text-xs text-slate-500">
          Demo: <span className="font-medium text-slate-700">coordenador@escola.local</span> /{' '}
          <span className="font-medium text-slate-700">coordenador</span>
        </p>
      </div>
    </div>
  )
}
