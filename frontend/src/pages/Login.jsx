// Autor: Cadu Spadari
// Alterações: Lucas Canto — visual novo, textos sem termos técnicos, nome do sistema igual aos documentos,
//             contas de demonstração escondidas em "Acesso de demonstração" e campo de senha com olho
//
// Tela de entrada: à esquerda a apresentação do sistema, à direita o formulário de login.
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { entrar, estaLogado, paginaInicial } from '../auth.js'
import { IconeEnviar, IconeLogo, IconePainel, IconeSino } from '../components/Icones.jsx'
import CampoSenha from '../components/CampoSenha.jsx'

const PASSOS = [
  { Icone: IconeEnviar, titulo: 'Envie notas e frequência' },
  { Icone: IconePainel, titulo: 'Veja quem precisa de atenção' },
  { Icone: IconeSino, titulo: 'Receba avisos' },
]

export default function Login({ onEntrou }) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  if (estaLogado()) {
    return <Navigate to={paginaInicial() ?? '/'} replace />
  }

  async function aoEnviarFormulario(e) {
    e.preventDefault() // impede o navegador de recarregar a página ao enviar o formulário
    setErro(null)
    setEnviando(true)
    try {
      await entrar(email, senha)
      onEntrou?.()
      navigate(paginaInicial() ?? '/', { replace: true })
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_0.9fr]">
      <aside className="lado-login flex flex-col justify-between gap-10">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 text-teal-300">
            <IconeLogo className="h-6 w-6" />
          </span>
          <p className="text-lg font-extrabold">Early-Warning de Evasão Escolar</p>
        </div>

        <div>
          <h1 className="max-w-lg text-4xl font-extrabold leading-tight tracking-tight">
            Perceba cedo quem está prestes a desistir da escola.
          </h1>
          <p className="mt-4 texto-intro-login">
            Juntamos frequência e notas para mostrar quais alunos precisam de apoio agora, antes que seja tarde.
          </p>

          <div className="mt-10 grid max-w-lg gap-3 text-sm">
            {PASSOS.map(({ Icone, titulo }) => (
              <div key={titulo} className="card-passo flex items-center gap-4">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-teal-300/15 text-teal-300">
                  <Icone className="h-5 w-5" />
                </span>
                <p className="titulo-passo">{titulo}</p>
              </div>
            ))}
          </div>
        </div>

        <p className="text-xs text-white/40">Feito para coordenadores, professores e gestores escolares.</p>
      </aside>

      <div className="flex items-center justify-center px-6 py-10">
        <div className="w-full max-w-md">
          <h2 className="titulo text-2xl">Bem-vindo de volta</h2>
          <p className="subtitulo">Entre com seu e-mail e senha.</p>

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
                placeholder="seu.nome@escola.com"
              />
            </label>
            <label className="rotulo">
              Senha
              <CampoSenha
                autoComplete="current-password"
                required
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
              />
            </label>

            {erro && <p className="rounded-xl bg-risco-alerta px-3 py-2 text-sm text-risco-alto">{erro}</p>}

            <button type="submit" disabled={enviando} className="btn-primario w-full py-3">
              {enviando ? 'Entrando…' : 'Entrar'}
            </button>
          </form>

          <p className="mt-4 text-center text-sm text-slate-500">
            Ainda não tem conta?{' '}
            <Link to="/cadastro" className="font-bold text-acento hover:underline">
              Criar conta
            </Link>
          </p>

          <details className="mt-6 rounded-xl border border-dashed border-linha bg-white/60 px-4 py-3 text-xs text-slate-500">
            <summary className="cursor-pointer font-semibold text-slate-600">Acesso de demonstração</summary>
            <div className="mt-2 space-y-1">
              <p>Coordenação: coordenador@escola.local · senha coordenador</p>
              <p>Professor: professor@escola.local · senha professor</p>
              <p>Código da escola (para criar conta): escola-2026</p>
            </div>
          </details>
        </div>
      </div>
    </div>
  )
}
