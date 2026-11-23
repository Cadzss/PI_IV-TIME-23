// Autor: Cadu Spadari
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, lerData } from '../api.js'
import BadgeRisco from '../components/BadgeRisco.jsx'
import { Carregando, Erro } from '../components/Estado.jsx'

function legendaEmail(email) {
  if (email.status === 'copia_local') {
    return ' — cópia em logs/emails.log (sem SMTP).'
  }
  if (email.status === 'falha') {
    return ` — falha: ${email.motivo}`
  }
  return ''
}

export default function Alertas() {
  const [alertas, setAlertas] = useState(null)
  const [erro, setErro] = useState(null)

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
    } catch (e) {
      setErro(e.message)
    }
  }

  if (erro) {
    return <Erro mensagem={erro} />
  }
  if (!alertas) {
    return <Carregando />
  }

  let naoLidos = 0
  let nivelAlto = 0
  let comEmail = 0
  for (const alerta of alertas) {
    if (!alerta.lido) naoLidos++
    if (alerta.nivelNovo === 'ALTO') nivelAlto++
    if (alerta.email) comEmail++
  }

  return (
    <section className="space-y-6">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-acento">Central de notificações</p>
        <h1 className="titulo">Alertas de risco</h1>
        <p className="subtitulo">
          Gerados no servidor quando o nível do aluno muda (especialmente entrada em ALTO).
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="kpi">
          <p className="text-xs font-bold uppercase text-slate-500">Total</p>
          <p className="mt-1 text-3xl font-extrabold">{alertas.length}</p>
        </div>
        <div className="kpi">
          <p className="text-xs font-bold uppercase text-risco-alto">Não lidos</p>
          <p className="mt-1 text-3xl font-extrabold text-risco-alto">{naoLidos}</p>
        </div>
        <div className="kpi">
          <p className="text-xs font-bold uppercase text-slate-500">Com e-mail / nível alto</p>
          <p className="mt-1 text-3xl font-extrabold">
            {comEmail} / {nivelAlto}
          </p>
        </div>
      </div>

      {alertas.length === 0 && (
        <div className="painel px-5 py-10 text-center text-slate-500">Nenhum alerta ainda.</div>
      )}

      <ul className="space-y-3">
        {alertas.map((alerta) => {
          const urgente = !alerta.lido && alerta.nivelNovo === 'ALTO'
          let classes = 'painel p-5'
          if (urgente) classes += ' border-risco-alto/40 bg-risco-alerta'
          if (alerta.lido) classes += ' opacity-70'

          return (
            <li key={alerta.id} className={classes}>
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <BadgeRisco nivel={alerta.nivelNovo} />
                    {!alerta.lido && <span className="chip bg-acento/10 text-acento">NOVO</span>}
                    {alerta.nivelAnterior && (
                      <span className="text-xs text-slate-500">
                        de {alerta.nivelAnterior} → {alerta.nivelNovo}
                      </span>
                    )}
                  </div>
                  <p className="font-bold text-tinta">{alerta.titulo}</p>
                  <p className="text-xs text-slate-500">
                    {lerData(alerta.criadoEm)?.toLocaleString('pt-BR')} · RA {alerta.matricula} ·{' '}
                    <Link to={`/alunos/${alerta.matricula}`} className="font-bold text-acento">
                      abrir aluno
                    </Link>
                  </p>
                  {alerta.email && (
                    <p className="text-xs text-slate-600">
                      E-mail ({alerta.email.status}): {alerta.email.destino}
                      {legendaEmail(alerta.email)}
                    </p>
                  )}
                </div>
                {!alerta.lido && (
                  <button type="button" onClick={() => marcarLido(alerta.id)} className="btn-secundario text-xs">
                    Marcar como lido
                  </button>
                )}
              </div>
            </li>
          )
        })}
      </ul>
    </section>
  )
}