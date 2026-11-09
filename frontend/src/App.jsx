import { NavLink, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard.jsx'
import DetalheAluno from './pages/DetalheAluno.jsx'
import Alertas from './pages/Alertas.jsx'
import Importar from './pages/Importar.jsx'

const links = [
  { to: '/', rotulo: 'Alunos por risco' },
  { to: '/alertas', rotulo: 'Alertas' },
  { to: '/importar', rotulo: 'Importar planilha' },
]

export default function App() {
  return (
    <div className="min-h-screen">
      <header className="bg-marinho text-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <span className="text-lg font-bold">Early-Warning Evasão</span>
          <nav className="flex gap-1 text-sm">
            {links.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end
                className={({ isActive }) =>
                  `rounded px-3 py-1.5 ${isActive ? 'bg-white/20 font-semibold' : 'hover:bg-white/10'}`
                }
              >
                {l.rotulo}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-6 py-8">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/alunos/:matricula" element={<DetalheAluno />} />
          <Route path="/alertas" element={<Alertas />} />
          <Route path="/importar" element={<Importar />} />
        </Routes>
      </main>
    </div>
  )
}
