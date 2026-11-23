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

  if (estaLogado()) {
    return <Navigate to="/" replace />
  }

  async function aoEnviarFormulario(e) {
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
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_0.9fr]">
      <aside className="lado-login">
        <p className="text-xs font-bold uppercase tracking-[0.22em] destaque-texto">
          Projeto Integrador IV · Time 23
        </p>
        <h1 className="mt-5 max-w-lg text-4xl font-extrabold leading-tight">
          Sistema de alerta precoce para evasão escolar
        </h1>
        <p className="mt-4 max-w-md texto-intro-login">
          O servidor Java calcula o risco com frequência, nota e tendência. O painel mostra quem precisa de
          acompanhamento agora.
        </p>

        <div className="mt-10 grid max-w-lg gap-3 text-sm">
          <div className="card-passo">
            <p className="titulo-passo">1. Importa CSV</p>
            <p className="texto-passo">Frequência e notas entram no MongoDB pelo Socket.</p>
          </div>
          <div className="card-passo">
            <p className="titulo-passo">2. Calcula risco</p>
            <p className="texto-passo">Regra no servidor: ALTO / MÉDIO / BAIXO + sinais.</p>
          </div>
          <div className="card-passo">
            <p className="titulo-passo">3. Gera alerta</p>
            <p className="texto-passo">Quando o aluno entra em ALTO, notifica a coordenação.</p>
          </div>
        </div>
      </aside>

      <div className="flex items-center justify-center px-6 py-10">
        <div className="w-full max-w-md">
          <h2 className="titulo text-2xl">Acesso ao painel</h2>
          <p className="subtitulo">Entre com e-mail e senha autenticados no servidor.</p>

          <form onSubmit={aoEnviarFormulario} className="painel mt-6 space-y-4 p-6">
            <label className="rotulo">
              E-mail
              <input
                type="email"
                autoComplete="username"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="campo"
                placeholder="coordenador@escola.local"
              />
            </label>
            <label className="rotulo">
              Senha
              <input
                type="password"
                autoComplete="current-password"
                required
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                className="campo"
              />
            </label>

            {erro && <p className="rounded-lg bg-risco-alerta px-3 py-2 text-sm text-risco-alto">{erro}</p>}

            <button type="submit" disabled={enviando} className="btn-primario w-full">
              {enviando ? 'Entrando…' : 'Entrar'}
            </button>
          </form>

          <p className="mt-4 text-center text-sm text-slate-500">
            Não tem conta?{' '}
            <Link to="/cadastro" className="font-bold text-acento">
              Criar conta
            </Link>
          </p>

          <div className="mt-5 rounded-xl border border-dashed border-linha bg-white p-4 text-xs text-slate-500">
            <p className="font-bold text-tinta">Contas demo (banca)</p>
            <p className="mt-2">coordenador@escola.local / coordenador</p>
            <p>professor@escola.local / professor</p>
            <p>Senha institucional: escola-2026</p>
          </div>
        </div>
      </div>
    </div>
  )
}