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
import { api } from '../api.js'
import BadgeRisco from '../components/BadgeRisco.jsx'
import { Carregando, Erro } from '../components/Estado.jsx'

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']
const COR_LINHA = '#1F3864'

function rotuloMes(data) {
  const [ano, mes] = data.split('-')
  return `${MESES[Number(mes) - 1]}/${ano.slice(2)}`
}

// Frequência (0–100%) e nota (0–10) têm escalas diferentes: dois gráficos com o mesmo eixo X,
// em vez de um gráfico com dois eixos Y.
function GraficoLinha({ titulo, dados, campo, dominio, formato, limite, rotuloLimite }) {
  return (
    <div>
      <h3 className="mb-1 text-sm font-semibold text-slate-600">{titulo}</h3>
      <ResponsiveContainer width="100%" height={180}>
        <LineChart data={dados} margin={{ top: 8, right: 16, bottom: 0, left: -8 }}>
          <CartesianGrid stroke="#e2e8f0" vertical={false} />
          <XAxis dataKey="mes" tick={{ fontSize: 12, fill: '#64748b' }} tickLine={false} axisLine={{ stroke: '#cbd5e1' }} />
          <YAxis
            domain={dominio}
            tickFormatter={formato}
            tick={{ fontSize: 12, fill: '#64748b' }}
            tickLine={false}
            axisLine={false}
            width={48}
          />
          <ReferenceLine
            y={limite}
            stroke="#94a3b8"
            strokeDasharray="4 4"
            label={{ value: rotuloLimite, position: 'insideBottomRight', fontSize: 11, fill: '#64748b' }}
          />
          <Tooltip
            formatter={(v) => [formato(v), titulo]}
            labelStyle={{ color: '#334155', fontWeight: 600 }}
            contentStyle={{ fontSize: 12, borderRadius: 6 }}
          />
          <Line
            type="monotone"
            dataKey={campo}
            stroke={COR_LINHA}
            strokeWidth={2}
            dot={{ r: 4, fill: COR_LINHA, stroke: '#fff', strokeWidth: 2 }}
            activeDot={{ r: 6 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

// Tela 2 — Detalhe do aluno: linha do tempo, sinais identificados e ações recomendadas
export default function DetalheAluno() {
  const { matricula } = useParams()
  const [aluno, setAluno] = useState(null)
  const [erro, setErro] = useState(null)
  const [acaoFeita, setAcaoFeita] = useState(null)

  useEffect(() => {
    api.detalharAluno(matricula).then(setAluno).catch((e) => setErro(e.message))
  }, [matricula])

  if (erro) return <Erro mensagem={erro} />
  if (!aluno) return <Carregando />

  const risco = aluno.risco ?? {}
  const dados = aluno.historico.map((r) => ({ ...r, mes: rotuloMes(r.data) }))

  return (
    <section className="space-y-6">
      <div className="rounded-lg bg-marinho px-5 py-3 text-sm font-semibold text-white">
        <Link to="/" className="hover:underline">
          &lt; Voltar
        </Link>
        <span className="mx-3 opacity-60">|</span>
        {aluno.nome} — {aluno.turma}
        <span className="ml-3 font-normal opacity-70">Matrícula {aluno.matricula}</span>
      </div>

      <div className="flex flex-wrap items-center gap-6">
        <BadgeRisco nivel={risco.nivel} grande prefixo="RISCO " />
        <div className="text-sm text-slate-600">
          Frequência atual <strong className="text-slate-800">{risco.frequenciaAtual?.toFixed(0)}%</strong>
          <span className="mx-3 text-slate-300">·</span>
          Nota atual <strong className="text-slate-800">{risco.notaAtual?.toFixed(1)}</strong>
          <span className="mx-3 text-slate-300">·</span>
          Pontuação de risco <strong className="text-slate-800">{risco.pontuacao}</strong>
        </div>
      </div>

      <div className="grid gap-6 rounded-lg bg-white p-5 shadow-sm md:grid-cols-2">
        <h2 className="font-semibold text-marinho md:col-span-2">Linha do tempo: frequência e notas</h2>
        <GraficoLinha
          titulo="Frequência"
          dados={dados}
          campo="frequencia"
          dominio={[0, 100]}
          formato={(v) => `${Number(v).toFixed(0)}%`}
          limite={75}
          rotuloLimite="mínimo 75%"
        />
        <GraficoLinha
          titulo="Nota média"
          dados={dados}
          campo="nota"
          dominio={[0, 10]}
          formato={(v) => Number(v).toFixed(1)}
          limite={5}
          rotuloLimite="nota 5,0"
        />
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="rounded-lg bg-white p-5 shadow-sm">
          <h2 className="mb-3 font-semibold text-marinho">Sinais identificados</h2>
          {risco.sinais?.length ? (
            <ul className="list-inside list-disc space-y-1 text-sm">
              {risco.sinais.map((s) => (
                <li key={s}>{s}</li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-500">Nenhum sinal de risco identificado.</p>
          )}
        </div>
        <div className="rounded-lg bg-white p-5 shadow-sm">
          <h2 className="mb-3 font-semibold text-marinho">Ações recomendadas</h2>
          <div className="flex flex-wrap gap-2">
            {risco.acoes?.map((a) => (
              <button
                key={a}
                onClick={() => setAcaoFeita(a)}
                className="rounded bg-marinho px-4 py-2 text-sm text-white hover:bg-marinho/90"
              >
                {a}
              </button>
            ))}
          </div>
          {acaoFeita && (
            <p className="mt-3 text-sm text-slate-600">
              Ação “{acaoFeita}” registrada. (O encaminhamento automático é uma funcionalidade futura.)
            </p>
          )}
        </div>
      </div>
    </section>
  )
}
