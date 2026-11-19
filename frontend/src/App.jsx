// Autor: Cadu Spadari
import { useState } from 'react'
import { NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { estaLogado, sair, sessaoAtual } from './auth.js'
import Dashboard from './pages/Dashboard.jsx'
import DetalheAluno from './pages/DetalheAluno.jsx'
import Alertas from './pages/Alertas.jsx'
import Importar from './pages/Importar.jsx'
import Login from './pages/Login.jsx'
import Cadastro from './pages/Cadastro.jsx'

const links = [
  { to: '/', rotulo: 'Alunos por risco' },
  { to: '/alertas', rotulo: 'Alertas' },
  { to: '/importar', rotulo: 'Importar planilha' },
]

export default function App() {
  const navigate = useNavigate()
  const [, setTick] = useState(0)
  const logado = estaLogado()
  const sessao = sessaoAtual()

  function atualizarSessao() {
    setTick((n) => n + 1)
  }

  function desconectar() {
    sair()
    atualizarSessao()
    navigate('/login', { replace: true })
  }

  if (!logado) {
    return (
      <Routes>
        <Route path="/login" element={<Login onEntrou={atualizarSessao} />} />
        <Route path="/cadastro" element={<Cadastro onEntrou={atualizarSessao} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <div className="min-h-screen">
      <header className="border-b-4 border-destaque bg-marinho text-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-4">
          <span className="text-lg font-bold">Early-Warning Evasão</span>
          <div className="flex flex-wrap items-center justify-end gap-3">
            <nav className="flex gap-1 text-sm">
              {links.map((l) => (
                <NavLink
                  key={l.to}
                  to={l.to}
                  end
                  className={({ isActive }) =>
                    `rounded px-3 py-1.5 ${isActive ? 'bg-destaque font-semibold' : 'hover:bg-white/10'}`
                  }
                >
                  {l.rotulo}
                </NavLink>
              ))}
            </nav>
            <div className="flex items-center gap-2 border-l border-white/20 pl-3 text-sm">
              <span className="hidden text-white/80 sm:inline">{sessao?.nome}</span>
              <button
                type="button"
                onClick={desconectar}
                className="rounded px-3 py-1.5 hover:bg-white/10"
              >
                Sair
              </button>
            </div>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-6 py-8">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/alunos/:matricula" element={<DetalheAluno />} />
          <Route path="/alertas" element={<Alertas />} />
          <Route path="/importar" element={<Importar />} />
          <Route path="/login" element={<Navigate to="/" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}
