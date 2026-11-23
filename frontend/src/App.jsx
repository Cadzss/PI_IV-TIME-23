// Autor: Cadu Spadari
import { useState } from 'react'
import { NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { estaLogado, FUNCOES, podeAcessar, sair, sessaoAtual } from './auth.js'
import Dashboard from './pages/Dashboard.jsx'
import DetalheAluno from './pages/DetalheAluno.jsx'
import Alertas from './pages/Alertas.jsx'
import Importar from './pages/Importar.jsx'
import Login from './pages/Login.jsx'
import Cadastro from './pages/Cadastro.jsx'

const MENUS = [
  { to: '/', rotulo: 'Painel de risco' },
  { to: '/alertas', rotulo: 'Alertas' },
  { to: '/importar', rotulo: 'Importar CSV' },
]

function ProtegerRota({ caminho, children }) {
  if (!podeAcessar(caminho)) {
    return <Navigate to="/" replace />
  }
  return children
}

export default function App() {
  const navigate = useNavigate()
  // Após login/cadastro o auth muda no localStorage; este número força o React a redesenhar.
  const [, setVersaoSessao] = useState(0)

  const logado = estaLogado()
  const sessao = sessaoAtual()

  function avisarQueEntrou() {
    setVersaoSessao((n) => n + 1)
  }

  function desconectar() {
    sair()
    avisarQueEntrou()
    navigate('/login', { replace: true })
  }

  if (!logado) {
    return (
      <Routes>
        <Route path="/login" element={<Login onEntrou={avisarQueEntrou} />} />
        <Route path="/cadastro" element={<Cadastro onEntrou={avisarQueEntrou} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  const menusVisiveis = MENUS.filter((item) => podeAcessar(item.to))

  return (
    <div className="min-h-screen">
      <header className="border-b border-linha bg-tinta text-white">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-6 py-4">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-acento">Early-Warning System</p>
            <h1 className="text-xl font-extrabold">Monitoramento de Evasão Escolar</h1>
          </div>

          <nav className="flex flex-wrap gap-1 text-sm">
            {menusVisiveis.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end
                className={({ isActive }) =>
                  isActive
                    ? 'rounded-lg px-3 py-2 font-semibold bg-acento text-white'
                    : 'rounded-lg px-3 py-2 font-semibold text-white/80 hover:bg-white/10'
                }
              >
                {item.rotulo}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-3 text-sm">
            <div className="hidden text-right sm:block">
              <p className="font-bold leading-tight">{sessao?.nome}</p>
              <p className="text-xs text-white/60">{FUNCOES[sessao?.funcao] || sessao?.funcao}</p>
            </div>
            <button
              type="button"
              onClick={desconectar}
              className="rounded-lg border border-white/20 px-3 py-1.5 hover:bg-white/10"
            >
              Sair
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-8">
        <Routes>
          <Route
            path="/"
            element={
              <ProtegerRota caminho="/">
                <Dashboard />
              </ProtegerRota>
            }
          />
          <Route
            path="/alunos/:matricula"
            element={
              <ProtegerRota caminho="/alunos">
                <DetalheAluno />
              </ProtegerRota>
            }
          />
          <Route
            path="/alertas"
            element={
              <ProtegerRota caminho="/alertas">
                <Alertas />
              </ProtegerRota>
            }
          />
          <Route
            path="/importar"
            element={
              <ProtegerRota caminho="/importar">
                <Importar />
              </ProtegerRota>
            }
          />
          <Route path="/login" element={<Navigate to="/" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}