// Autor: Cadu Spadari
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api, lerData } from '../api.js'
import BadgeRisco from '../components/BadgeRisco.jsx'
import { Carregando, Erro } from '../components/Estado.jsx'

const NOME_MES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

function rotuloMes(dataIso) {
  const [ano, mes] = dataIso.split('-')
  return `${NOME_MES[Number(mes) - 1]}/${ano.slice(2)}`
}

function formatoPercentual(valor) {
  return `${Number(valor).toFixed(0)}%`
}

function formatoNota(valor) {
  return Number(valor).toFixed(1)
}

function GraficoLinha({ titulo, dados, campo, minY, maxY, limite, rotuloLimite, cor, formatar }) {
  return (
    <div>
      <h3 className="mb-2 text-sm font-bold text-slate-600">{titulo}</h3>
      <ResponsiveContainer width="100%" height={210}>
        <LineChart data={dados} margin={{ top: 8, right: 12, bottom: 0, left: -8 }}>
          <CartesianGrid stroke="#E4E7EC" vertical={false} />
          <XAxis
            dataKey="mes"
            tick={{ fontSize: 12, fill: '#667085' }}
            tickLine={false}
            axisLine={{ stroke: '#D0D5DD' }}
          />
          <YAxis
            domain={[minY, maxY]}
            tickFormatter={formatar}
            tick={{ fontSize: 12, fill: '#667085' }}
            tickLine={false}
            axisLine={false}
            width={48}
          />
          <ReferenceLine
            y={limite}
            stroke="#98A2B3"
            strokeDasharray="4 4"
            label={{
              value: rotuloLimite,
              position: 'insideBottomRight',
              fontSize: 11,
              fill: '#667085',
            }}
          />
          <Tooltip formatter={(v) => [formatar(v), titulo]} contentStyle={{ fontSize: 12, borderRadius: 8 }} />
          <Line
            type="monotone"
            dataKey={campo}
            stroke={cor}
            strokeWidth={2.5}
            dot={{ r: 4, fill: cor, stroke: '#fff', strokeWidth: 2 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

export default function DetalheAluno() {
  const { matricula } = useParams()
  const [aluno, setAluno] = useState(null)
  const [erro, setErro] = useState(null)
  const [acaoFeita, setAcaoFeita] = useState(null)

  useEffect(() => {
    api.detalharAluno(matricula)
      .then(setAluno)
      .catch((e) => setErro(e.message))
  }, [matricula])

  if (erro) {
    return <Erro mensagem={erro} />
  }
  if (!aluno) {
    return <Carregando />
  }

  const risco = aluno.risco ?? {}
  const historico = [...(aluno.historico || [])].sort((a, b) => a.data.localeCompare(b.data))
  const pontosGrafico = historico.map((linha) => ({
    ...linha,
    mes: rotuloMes(linha.data),
  }))

  return (
    <section className="space-y-6">
      <div className="painel overflow-hidden">
        <div className="bg-tinta px-5 py-5 text-white">
          <Link to="/" className="text-xs font-bold uppercase tracking-wide text-white/60 hover:text-white">
            ← Voltar ao painel
          </Link>
          <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
            <div>
              <h1 className="text-3xl font-extrabold">{aluno.nome}</h1>
              <p className="mt-1 text-sm text-white/70">
                RA {aluno.matricula} · Turma {aluno.turma}
              </p>
            </div>
            <BadgeRisco nivel={risco.nivel} grande />
          </div>
        </div>
        <div className="grid gap-3 border-t border-linha p-5 sm:grid-cols-4">
          <div>
            <p className="text-xs font-bold uppercase text-slate-500">Frequência</p>
            <p className="mt-1 text-2xl font-extrabold">{risco.frequenciaAtual?.toFixed(0)}%</p>
          </div>
          <div>
            <p className="text-xs font-bold uppercase text-slate-500">Nota</p>
            <p className="mt-1 text-2xl font-extrabold">{risco.notaAtual?.toFixed(1)}</p>
          </div>
          <div>
            <p className="text-xs font-bold uppercase text-slate-500">Pontuação</p>
            <p className="mt-1 text-2xl font-extrabold">{risco.pontuacao}</p>
          </div>
          <div>
            <p className="text-xs font-bold uppercase text-slate-500">Calculado em</p>
            <p className="mt-1 text-sm font-bold">
              {risco.calculadoEm ? lerData(risco.calculadoEm)?.toLocaleString('pt-BR') : '—'}
            </p>
          </div>
        </div>
      </div>

      <div className="painel grid gap-6 p-5 md:grid-cols-2">
        <h2 className="text-lg font-extrabold md:col-span-2">Evolução no tempo</h2>
        <GraficoLinha
          titulo="Frequência (%)"
          dados={pontosGrafico}
          campo="frequencia"
          minY={0}
          maxY={100}
          formatar={formatoPercentual}
          limite={75}
          rotuloLimite="mín. 75%"
          cor="#0F7B8A"
        />
        <GraficoLinha
          titulo="Nota (0–10)"
          dados={pontosGrafico}
          campo="nota"
          minY={0}
          maxY={10}
          formatar={formatoNota}
          limite={5}
          rotuloLimite="nota 5,0"
          cor="#102A43"
        />
      </div>

      <div className="painel overflow-hidden">
        <div className="border-b border-linha px-5 py-3">
          <h2 className="text-lg font-extrabold">Histórico importado</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[520px] text-left text-sm">
            <thead className="bg-fundo text-xs uppercase text-slate-500">
              <tr>
                <th className="px-5 py-3 font-bold">Período</th>
                <th className="px-5 py-3 text-right font-bold">Frequência</th>
                <th className="px-5 py-3 text-right font-bold">Nota</th>
              </tr>
            </thead>
            <tbody>
              {historico.map((linha) => (
                <tr key={linha.data} className="border-t border-linha">
                  <td className="px-5 py-3">{linha.data}</td>
                  <td className="px-5 py-3 text-right tabular-nums">{Number(linha.frequencia).toFixed(0)}%</td>
                  <td className="px-5 py-3 text-right tabular-nums">{Number(linha.nota).toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="painel p-5">
          <h2 className="text-lg font-extrabold">Sinais identificados pelo servidor</h2>
          {risco.sinais?.length ? (
            <ul className="mt-3 space-y-2">
              {risco.sinais.map((sinal) => (
                <li key={sinal} className="rounded-lg border border-linha bg-fundo px-3 py-2 text-sm">
                  {sinal}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-slate-500">Nenhum sinal de risco.</p>
          )}
        </div>
        <div className="painel p-5">
          <h2 className="text-lg font-extrabold">Ações recomendadas</h2>
          <div className="mt-3 flex flex-wrap gap-2">
            {risco.acoes?.map((acao) => (
              <button key={acao} type="button" onClick={() => setAcaoFeita(acao)} className="btn-primario">
                {acao}
              </button>
            ))}
          </div>
          {acaoFeita && (
            <p className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-risco-baixo">
              Ação “{acaoFeita}” registrada nesta sessão (encaminhamento automático fica para evolução futura).
            </p>
          )}
        </div>
      </div>
    </section>
  )
}