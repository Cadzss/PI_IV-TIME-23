// Autor: Breno Rogeri Miquelino
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, lerData } from '../api.js'
import BadgeRisco from '../components/BadgeRisco.jsx'
import { Carregando, Erro } from '../components/Estado.jsx'

function media(lista, campo) {
  if (!lista?.length) return null
  const soma = lista.reduce((acc, a) => acc + (Number(a.risco?.[campo]) || 0), 0)
  return soma / lista.length
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [turmas, setTurmas] = useState([])
  const [turma, setTurma] = useState('')
  const [nivel, setNivel] = useState('')
  const [busca, setBusca] = useState('')
  const [alunos, setAlunos] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    api.listarTurmas().then(setTurmas).catch((e) => setErro(e.message))
  }, [])

  useEffect(() => {
    setAlunos(null)
    api.listarAlunos({ turma, nivel }).then((lista) => {
      setAlunos(lista)
      setErro(null)
    }).catch((e) => setErro(e.message))
  }, [turma, nivel])

  const filtrados = useMemo(() => {
    if (!alunos) return null
    const q = busca.trim().toLowerCase()
    if (!q) return alunos
    return alunos.filter((a) => a.nome?.toLowerCase().includes(q) || String(a.matricula).toLowerCase().includes(q))
  }, [alunos, busca])

  const contagem = (n) => filtrados?.filter((a) => a.risco?.nivel === n).length ?? 0
  const mediaFreq = media(filtrados, 'frequenciaAtual')
  const mediaNota = media(filtrados, 'notaAtual')
  const pctAlto = filtrados?.length ? Math.round((contagem('ALTO') / filtrados.length) * 100) : 0

  return (
    <section className="space-y-6">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-acento">Painel operacional</p>
        <h1 className="titulo">Alunos por risco de evasão</h1>
        <p className="subtitulo">React → Ponte HTTP → Servidor Socket → MongoDB. Ordenação: alto → médio → baixo.</p>
      </div>

      <div className="painel grid gap-3 p-4 md:grid-cols-4">
        <label className="rotulo md:col-span-2">
          Buscar por RA ou nome
          <input className="campo" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Ex.: 2026001 ou Ana" />
        </label>
        <label className="rotulo">
          Turma
          <select className="campo" value={turma} onChange={(e) => setTurma(e.target.value)}>
            <option value="">Todas</option>
            {turmas.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label className="rotulo">
          Nível
          <select className="campo" value={nivel} onChange={(e) => setNivel(e.target.value)}>
            <option value="">Todos</option>
            <option value="ALTO">Alto</option>
            <option value="MEDIO">Médio</option>
            <option value="BAIXO">Baixo</option>
          </select>
        </label>
      </div>

      {filtrados && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <div className="kpi">
            <p className="text-xs font-bold uppercase text-slate-500">Alunos na lista</p>
            <p className="mt-1 text-3xl font-extrabold">{filtrados.length}</p>
            <p className="mt-1 text-xs text-slate-500">{turmas.length} turma(s) no banco</p>
          </div>
          <div className="kpi">
            <p className="text-xs font-bold uppercase text-risco-alto">Risco alto</p>
            <p className="mt-1 text-3xl font-extrabold text-risco-alto">{contagem('ALTO')}</p>
            <p className="mt-1 text-xs text-slate-500">{pctAlto}% do filtro atual</p>
          </div>
          <div className="kpi">
            <p className="text-xs font-bold uppercase text-risco-medio">Risco médio</p>
            <p className="mt-1 text-3xl font-extrabold text-risco-medio">{contagem('MEDIO')}</p>
          </div>
          <div className="kpi">
            <p className="text-xs font-bold uppercase text-slate-500">Frequência média</p>
            <p className="mt-1 text-3xl font-extrabold">{mediaFreq == null ? '—' : `${mediaFreq.toFixed(0)}%`}</p>
          </div>
          <div className="kpi">
            <p className="text-xs font-bold uppercase text-slate-500">Nota média</p>
            <p className="mt-1 text-3xl font-extrabold">{mediaNota == null ? '—' : mediaNota.toFixed(1)}</p>
          </div>
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_280px]">
        <div>
          {erro && <Erro mensagem={erro} />}
          {!erro && !alunos && <Carregando />}
          {filtrados && (
            <div className="painel overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[920px] text-left text-sm">
                  <thead className="border-b border-linha bg-fundo text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-4 py-3 font-bold">RA</th>
                      <th className="px-4 py-3 font-bold">Aluno</th>
                      <th className="px-4 py-3 font-bold">Turma</th>
                      <th className="px-4 py-3 text-right font-bold">Freq.</th>
                      <th className="px-4 py-3 text-right font-bold">Nota</th>
                      <th className="px-4 py-3 text-right font-bold">Pontos</th>
                      <th className="px-4 py-3 font-bold">Risco</th>
                      <th className="px-4 py-3 font-bold">Principal sinal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtrados.length === 0 && (
                      <tr><td colSpan={8} className="px-4 py-10 text-center text-slate-500">Nenhum aluno neste filtro.</td></tr>
                    )}
                    {filtrados.map((a) => (
                      <tr
                        key={a.matricula}
                        onClick={() => navigate(`/alunos/${a.matricula}`)}
                        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/alunos/${a.matricula}`) } }}
                        role="link"
                        tabIndex={0}
                        className="cursor-pointer border-b border-linha hover:bg-fundo"
                      >
                        <td className="px-4 py-3 tabular-nums text-slate-500">{a.matricula}</td>
                        <td className="px-4 py-3 font-bold">{a.nome}</td>
                        <td className="px-4 py-3">{a.turma}</td>
                        <td className="px-4 py-3 text-right tabular-nums">{a.risco?.frequenciaAtual?.toFixed(0)}%</td>
                        <td className="px-4 py-3 text-right tabular-nums">{a.risco?.notaAtual?.toFixed(1)}</td>
                        <td className="px-4 py-3 text-right font-bold tabular-nums">{a.risco?.pontuacao ?? '—'}</td>
                        <td className="px-4 py-3"><BadgeRisco nivel={a.risco?.nivel} /></td>
                        <td className="max-w-[240px] truncate px-4 py-3 text-xs text-slate-600">{a.risco?.sinais?.[0] || 'Sem sinal crítico'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <p className="border-t border-linha px-4 py-3 text-xs text-slate-500">Clique na linha para ver histórico, sinais e ações.</p>
            </div>
          )}
        </div>

        <aside className="space-y-4">
          <div className="painel p-4">
            <h2 className="text-sm font-extrabold">Como o risco é calculado</h2>
            <ul className="mt-3 space-y-2 text-xs leading-relaxed text-slate-600">
              <li>Frequência &lt; 75% → +3</li>
              <li>Frequência &lt; 85% → +1</li>
              <li>Nota &lt; 5,0 → +2</li>
              <li>Nota &lt; 7,0 → +1</li>
              <li>Queda de nota ≥ 1,0 → +1</li>
              <li>Queda de frequência ≥ 10 p.p. → +1</li>
            </ul>
            <div className="mt-4 space-y-1 text-xs font-bold">
              <p className="text-risco-alto">≥ 4 = ALTO</p>
              <p className="text-risco-medio">2–3 = MÉDIO</p>
              <p className="text-risco-baixo">0–1 = BAIXO</p>
            </div>
          </div>
          <div className="painel p-4">
            <h2 className="text-sm font-extrabold">Arquitetura</h2>
            <p className="mt-2 text-xs leading-relaxed text-slate-600">React → Ponte (:8080) → Servidor (:12345) → MongoDB (:27017)</p>
            <p className="mt-2 text-xs text-slate-500">
              Último cálculo: {filtrados?.[0]?.risco?.calculadoEm ? lerData(filtrados[0].risco.calculadoEm)?.toLocaleString('pt-BR') : '—'}
            </p>
          </div>
        </aside>
      </div>
    </section>
  )
}
