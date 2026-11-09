import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, lerData } from '../api.js'
import { Carregando, Erro } from '../components/Estado.jsx'

const corPonto = { ALTO: 'bg-risco-alto', MEDIO: 'bg-risco-medio', BAIXO: 'bg-risco-baixo' }

// Tela 3 — Central de alertas: notificações sempre que um aluno entra em risco ou muda de nível
export default function Alertas() {
  const [alertas, setAlertas] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    api.listarAlertas().then(setAlertas).catch((e) => setErro(e.message))
  }, [])

  async function marcarLido(id) {
    try {
      await api.marcarAlertaLido(id)
      setAlertas((lista) => lista.map((a) => (a.id === id ? { ...a, lido: true } : a)))
    } catch (e) {
      setErro(e.message)
    }
  }

  if (erro) return <Erro mensagem={erro} />
  if (!alertas) return <Carregando />

  const naoLidos = alertas.filter((a) => !a.lido).length

  return (
    <section>
      <h1 className="text-2xl font-bold text-marinho">Notificações</h1>
      <p className="mb-6 mt-1 text-sm text-slate-500">{naoLidos} não lida(s)</p>

      {alertas.length === 0 && <p className="text-slate-500">Nenhum alerta por enquanto.</p>}

      <ul className="space-y-3">
        {alertas.map((a) => {
          const destaque = !a.lido && a.nivelNovo === 'ALTO'
          return (
            <li
              key={a.id}
              className={`flex items-start gap-4 rounded-lg p-4 ${destaque ? 'bg-orange-100' : 'bg-white'} ${
                a.lido ? 'opacity-60' : ''
              }`}
            >
              <span className={`mt-1 h-5 w-5 shrink-0 rounded-full ${corPonto[a.nivelNovo] ?? 'bg-slate-400'}`} />
              <div className="flex-1">
                <p className="font-semibold">{a.titulo}</p>
                <p className="mt-1 text-xs text-slate-500">
                  {lerData(a.criadoEm)?.toLocaleString('pt-BR')} ·{' '}
                  <Link to={`/alunos/${a.matricula}`} className="text-marinho underline">
                    ver aluno
                  </Link>
                </p>
                {a.email && (
                  <p className="mt-1 text-xs text-slate-600">
                    E-mail para {a.email.destino}
                    {a.email.status === 'enviado'
                      ? ' enviado ao coordenador.'
                      : a.email.status === 'falha'
                        ? `: falha no envio (${a.email.motivo}).`
                        : ' registrado neste alerta. Defina SMTP_HOST para o envio real.'}
                  </p>
                )}
              </div>
              {!a.lido && (
                <button onClick={() => marcarLido(a.id)} className="text-xs text-slate-500 hover:text-slate-800">
                  Marcar como lido
                </button>
              )}
            </li>
          )
        })}
      </ul>
    </section>
  )
}
