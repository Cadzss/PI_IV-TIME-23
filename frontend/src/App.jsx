// Autor: Cadu Spadari
// Alterações: Lucas Canto — menu com contador de avisos não lidos, Minha conta, Equipe, faixa de senha
//             temporária, rotas protegidas pela função do usuário e visual novo do cabeçalho
//
// Componente raiz: decide entre as telas de fora (login/cadastro) e as de dentro (com cabeçalho e menu).
import { useEffect, useState } from 'react'
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { api } from './api.js'
import { estaLogado, FUNCOES, paginaInicial, podeAcessar, sair, sessaoAtual } from './auth.js'
import Dashboard from './pages/Dashboard.jsx'
import DetalheAluno from './pages/DetalheAluno.jsx'
import Alertas from './pages/Alertas.jsx'
import Importar from './pages/Importar.jsx'
import Login from './pages/Login.jsx'
import Cadastro from './pages/Cadastro.jsx'
import MinhaConta from './pages/MinhaConta.jsx'
import Equipe from './pages/Equipe.jsx'
import {
  IconeAtencao,
  IconeEnviar,
  IconeLogo,
  IconePainel,
  IconePessoa,
  IconeSair,
  IconeSino,
} from './components/Icones.jsx'

const MENUS = [
  { to: '/', rotulo: 'Painel', Icone: IconePainel },
  { to: '/alertas', rotulo: 'Avisos', Icone: IconeSino, contador: true },
  { to: '/importar', rotulo: 'Enviar planilha', Icone: IconeEnviar },
  { to: '/equipe', rotulo: 'Equipe', Icone: IconePessoa },
]

/** Só deixa abrir a página se a função do usuário tiver acesso; senão manda para o início. */
function ProtegerRota({ children }) {
  const { pathname } = useLocation()
  if (!podeAcessar(pathname)) {
    return <Navigate to={paginaInicial()} replace />
  }
  return children
}

/** "Maria Souza" -> "MS" (usado na bolinha do canto superior direito). */
function iniciais(nome) {
  return (nome || '?')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join('')
}

/** Quantos avisos ainda não foram lidos (só para quem vê os avisos). Atualiza ao trocar de página. */
function useAvisosNaoLidos(ativo) {
  const { pathname } = useLocation()
  const [naoLidos, setNaoLidos] = useState(0)

  useEffect(() => {
    if (!ativo) return undefined
    let cancelado = false
    const buscar = () =>
      api.contarAvisosNaoLidos()
        .then((r) => { if (!cancelado) setNaoLidos(r.naoLidos) })
        .catch(() => {})
    buscar()
    window.addEventListener('avisos-atualizados', buscar)
    return () => {
      cancelado = true
      window.removeEventListener('avisos-atualizados', buscar)
    }
  }, [ativo, pathname])

  return naoLidos
}

export default function App() {
  const navigate = useNavigate()
  // Após login/cadastro o auth muda no localStorage; este número força o React a redesenhar.
  const [, setVersaoSessao] = useState(0)

  const logado = estaLogado()
  const sessao = sessaoAtual()
  const naoLidos = useAvisosNaoLidos(logado && podeAcessar('/alertas'))

  useEffect(() => {
    const redesenhar = () => setVersaoSessao((n) => n + 1)
    window.addEventListener('sessao-atualizada', redesenhar)
    return () => window.removeEventListener('sessao-atualizada', redesenhar)
  }, [])

  function avisarQueEntrou() {
    setVersaoSessao((n) => n + 1)
  }

  function desconectar() {
    sair()
    avisarQueEntrou()
    navigate('/login', { replace: true })
  }

  // Não logado: só existem as telas de entrar e de criar conta
  if (!logado) {
    return (
      <Routes>
        <Route path="/login" element={<Login onEntrou={avisarQueEntrou} />} />
        <Route path="/cadastro" element={<Cadastro onEntrou={avisarQueEntrou} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  const inicio = paginaInicial()
  const menusVisiveis = MENUS.filter((item) => podeAcessar(item.to))

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 bg-tinta text-white shadow-lg shadow-tinta/10">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <Link to="/" className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 text-teal-300">
              <IconeLogo className="h-6 w-6" />
            </span>
            <div className="leading-tight">
              <p className="text-base font-extrabold">Early-Warning</p>
              <p className="text-xs text-white/60">Evasão Escolar</p>
            </div>
          </Link>

          <nav className="order-3 flex w-full gap-1 overflow-x-auto text-sm md:order-none md:w-auto">
            {menusVisiveis.map(({ to, rotulo, Icone, contador }) => (
              <NavLink
                key={to}
                to={to}
                end
                className={({ isActive }) =>
                  `flex items-center gap-2 whitespace-nowrap rounded-xl px-3.5 py-2 font-semibold transition ${
                    isActive ? 'bg-white text-tinta' : 'text-white/75 hover:bg-white/10 hover:text-white'
                  }`
                }
              >
                <Icone className="h-4 w-4" />
                {rotulo}
                {contador && naoLidos > 0 && (
                  <span
                    className="flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-[#C0261B] px-1.5 text-[11px] font-extrabold text-white"
                    title={`${naoLidos} aviso(s) não lido(s)`}
                  >
                    {naoLidos > 99 ? '99+' : naoLidos}
                  </span>
                )}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2 text-sm">
            <NavLink
              to="/conta"
              title="Minha conta"
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-2 py-1.5 transition ${isActive ? 'bg-white/15' : 'hover:bg-white/10'}`
              }
            >
              <div className="hidden text-right sm:block">
                <p className="font-bold leading-tight">{sessao?.nome}</p>
                <p className="text-xs text-white/60">{FUNCOES[sessao?.funcao] || sessao?.funcao}</p>
              </div>
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-teal-400/20 text-xs font-extrabold text-teal-200">
                {iniciais(sessao?.nome)}
              </span>
            </NavLink>
            <button
              type="button"
              onClick={desconectar}
              title="Sair"
              className="flex items-center gap-1.5 rounded-xl px-2.5 py-2 text-white/75 transition hover:bg-white/10 hover:text-white"
            >
              <IconeSair className="h-4 w-4" />
              <span className="hidden sm:inline">Sair</span>
            </button>
          </div>
        </div>
      </header>

      {sessao?.senhaTemporaria && (
        <div className="border-b border-amber-200 bg-amber-50">
          <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-2 px-4 py-2.5 text-sm text-risco-medio sm:px-6">
            <IconeAtencao className="h-4 w-4 shrink-0" />
            Você entrou com uma senha temporária.
            <Link to="/conta" className="font-bold underline">Crie uma senha nova em Minha conta</Link>
          </div>
        </div>
      )}

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
        <Routes>
          <Route path="/" element={<ProtegerRota><Dashboard /></ProtegerRota>} />
          <Route path="/alunos/:matricula" element={<ProtegerRota><DetalheAluno /></ProtegerRota>} />
          <Route path="/alertas" element={<ProtegerRota><Alertas /></ProtegerRota>} />
          <Route path="/importar" element={<ProtegerRota><Importar /></ProtegerRota>} />
          <Route path="/equipe" element={<ProtegerRota><Equipe /></ProtegerRota>} />
          <Route path="/conta" element={<ProtegerRota><MinhaConta /></ProtegerRota>} />
          <Route path="/login" element={<Navigate to={inicio} replace />} />
          <Route path="*" element={<Navigate to={inicio} replace />} />
        </Routes>
      </main>

      <footer className="mx-auto max-w-7xl px-4 pb-8 text-xs text-slate-400 sm:px-6">
        Early-Warning de Evasão Escolar · ajudando a escola a agir antes que o aluno desista.
      </footer>
    </div>
  )
}
