// Autor: Cadu Spadari
// Alterações: Lucas Canto — filtros, card do relatório semanal, status do e-mail em linguagem simples
//             e atualização do contador de não lidos do menu
//
// Tela 3 do protótipo: central de avisos (mudanças de risco e relatório semanal).
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import BadgeRisco from '../components/BadgeRisco.jsx'
import { Carregando, Erro } from '../components/Estado.jsx'
import { IconeArquivo, IconeCheck, IconeSino } from '../components/Icones.jsx'
import { formatarDataHora } from '../formatos.js'

// Traduz o status técnico do e-mail (gravado pelo servidor) para uma frase que o usuário entende
const SITUACAO_EMAIL = {
  enviado: { texto: 'Coordenação avisada por e-mail', classe: 'text-risco-baixo' },
  pendente: { texto: 'Enviando e-mail para a coordenação…', classe: 'text-slate-500' },
  copia_local: { texto: 'E-mail não enviado: o envio de e-mails ainda não foi configurado', classe: 'text-slate-500' },
  falha: { texto: 'Não foi possível enviar o e-mail para a coordenação', classe: 'text-risco-alto' },
}

const FILTROS = [
  { valor: 'todos', rotulo: 'Todos' },
  { valor: 'naoLidos', rotulo: 'Não lidos' },
  { valor: 'urgentes', rotulo: 'Risco alto' },
  { valor: 'relatorios', rotulo: 'Relatórios semanais' },
]

const RELATORIO = 'RELATORIO_SEMANAL'

function Numero({ valor, rotulo, classe = 'text-tinta' }) {
  return (
    <div className="rounded-xl bg-fundo/70 px-3 py-2 text-center">
      <p className={`text-lg font-extrabold leading-none ${classe}`}>{valor ?? 0}</p>
      <p className="mt-1 text-[11px] font-semibold text-slate-500">{rotulo}</p>
    </div>
  )
}

export default function Alertas() {
  const [alertas, setAlertas] = useState(null)
  const [erro, setErro] = useState(null)
  const [filtro, setFiltro] = useState('todos')

  useEffect(() => {
    api.listarAlertas()
      .then(setAlertas)
      .catch((e) => setErro(e.message))
  }, [])

  async function marcarLido(id) {
    try {
      await api.marcarAlertaLido(id)
      setAlertas((lista) =>
        lista.map((alerta) => (alerta.id === id ? { ...alerta, lido: true } : alerta)),
      )
      window.dispatchEvent(new Event('avisos-atualizados'))
    } catch (e) {
      setErro(e.message)
    }
  }

  if (erro) {
    return <Erro mensagem={erro} />
  }
  if (!alertas) {
    return <Carregando texto="Carregando avisos…" />
  }

  const naoLidos = alertas.filter((a) => !a.lido).length
  const urgentes = alertas.filter((a) => a.nivelNovo === 'ALTO').length
  const visiveis = alertas.filter((a) => {
    if (filtro === 'naoLidos') return !a.lido
    if (filtro === 'urgentes') return a.nivelNovo === 'ALTO'
    if (filtro === 'relatorios') return a.tipo === RELATORIO
    return true
  })

  return (
    <section className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="titulo">Avisos</h1>
        <p className="subtitulo">Alunos que mudaram de situação e o resumo semanal, com os casos urgentes em destaque.</p>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="kpi">
          <p className="kpi-rotulo">Total de avisos</p>
          <p className="kpi-valor">{alertas.length}</p>
        </div>
        <div className="kpi border-l-4 border-l-[#0F7B8A]">
          <p className="kpi-rotulo">Não lidos</p>
          <p className="kpi-valor text-acento">{naoLidos}</p>
        </div>
        <div className="kpi border-l-4 border-l-[#B42318]">
          <p className="kpi-rotulo">Entraram em risco alto</p>
          <p className="kpi-valor text-risco-alto">{urgentes}</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {FILTROS.map((f) => (
          <button
            key={f.valor}
            type="button"
            onClick={() => setFiltro(f.valor)}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
              filtro === f.valor ? 'bg-tinta text-white' : 'bg-white text-slate-600 ring-1 ring-linha hover:text-tinta'
            }`}
          >
            {f.rotulo}
          </button>
        ))}
      </div>

      {visiveis.length === 0 && (
        <div className="painel flex flex-col items-center gap-3 px-5 py-14 text-center text-slate-500">
          <IconeSino className="h-8 w-8 text-slate-300" />
          {alertas.length === 0 ? 'Nenhum aviso por enquanto.' : 'Nenhum aviso neste filtro.'}
        </div>
      )}

      <ul className="space-y-3">
        {visiveis.map((alerta) => {
          if (alerta.tipo === RELATORIO) {
            const r = alerta.resumo ?? {}
            return (
              <li key={alerta.id} className={`painel border-l-4 border-l-[#4E8B32] p-5 ${alerta.lido ? 'opacity-60' : ''}`}>
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="min-w-0 flex-1 space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50 text-risco-baixo">
                        <IconeArquivo className="h-4 w-4" />
                      </span>
                      <p className="font-bold text-tinta">{alerta.titulo}</p>
                      {!alerta.lido && <span className="chip bg-acento/10 text-acento">Novo</span>}
                    </div>
                    <div className="grid max-w-lg grid-cols-3 gap-2 sm:grid-cols-6">
                      <Numero valor={r.total} rotulo="alunos" />
                      <Numero valor={r.alto} rotulo="risco alto" classe="text-risco-alto" />
                      <Numero valor={r.medio} rotulo="risco médio" classe="text-risco-medio" />
                      <Numero valor={r.baixo} rotulo="risco baixo" classe="text-risco-baixo" />
                      <Numero valor={r.pioraram} rotulo="pioraram" />
                      <Numero valor={r.melhoraram} rotulo="melhoraram" />
                    </div>
                    <p className="text-sm text-slate-600">{alerta.descricao}</p>
                    <p className="text-xs text-slate-500">{formatarDataHora(alerta.criadoEm)}</p>
                  </div>
                  <div className="flex shrink-0 flex-wrap gap-2">
                    <Link to="/" className="btn-secundario text-xs">Ver painel</Link>
                    {!alerta.lido && (
                      <button type="button" onClick={() => marcarLido(alerta.id)} className="btn-primario text-xs">
                        <IconeCheck className="h-4 w-4" />
                        Marcar como lido
                      </button>
                    )}
                  </div>
                </div>
              </li>
            )
          }

          const urgente = !alerta.lido && alerta.nivelNovo === 'ALTO'
          const email = alerta.email && SITUACAO_EMAIL[alerta.email.status]
          let classes = 'painel p-5 transition'
          if (urgente) classes += ' border-l-4 border-l-[#B42318]'
          if (alerta.lido) classes += ' opacity-60'

          return (
            <li key={alerta.id} className={classes}>
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0 flex-1 space-y-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <BadgeRisco nivel={alerta.nivelNovo} />
                    {!alerta.lido && <span className="chip bg-acento/10 text-acento">Novo</span>}
                  </div>
                  <p className="font-bold text-tinta">{alerta.titulo}</p>
                  <p className="text-xs text-slate-500">
                    {formatarDataHora(alerta.criadoEm)} · Matrícula {alerta.matricula}
                  </p>
                  {email && <p className={`text-xs ${email.classe}`}>{email.texto}</p>}
                </div>
                <div className="flex shrink-0 flex-wrap gap-2">
                  <Link to={`/alunos/${encodeURIComponent(alerta.matricula)}`} className="btn-secundario text-xs">
                    Ver aluno
                  </Link>
                  {!alerta.lido && (
                    <button type="button" onClick={() => marcarLido(alerta.id)} className="btn-primario text-xs">
                      <IconeCheck className="h-4 w-4" />
                      Marcar como lido
                    </button>
                  )}
                </div>
              </div>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
