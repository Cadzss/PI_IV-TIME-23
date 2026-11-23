// Autor: Cadu Spadari
// Alterações: Lucas Canto — gráfico único Frequência x Notas (últimos 3 meses) igual ao protótipo,
//             engajamento, correção de lançamentos (adicionar/corrigir/excluir), providências a partir
//             das ações recomendadas e remoção do aluno (coordenador)
//
// Tela 2 do protótipo: tudo sobre um aluno — risco, linha do tempo, sinais, ações e histórico.
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api } from '../api.js'
import { sessaoAtual } from '../auth.js'
import BadgeRisco from '../components/BadgeRisco.jsx'
import { Carregando, Erro } from '../components/Estado.jsx'
import { IconeAtencao, IconeCheck, IconeVoltar } from '../components/Icones.jsx'
import {
  formatarData,
  formatarDataHora,
  formatarMesCurto,
  formatarNota,
  formatarPercentual,
  NIVEIS,
} from '../formatos.js'

/** Registros dos últimos 3 meses, contados a partir do registro mais recente. */
function ultimosTresMeses(historico) {
  if (!historico.length) return historico
  const [ano, mes, dia] = historico[historico.length - 1].data.split('-').map(Number)
  const corte = new Date(Date.UTC(ano, mes - 1 - 3, dia)).toISOString().slice(0, 10)
  return historico.filter((linha) => linha.data > corte)
}

const CORES_TOPO = {
  ALTO: 'from-[#7a1f17] to-tinta',
  MEDIO: 'from-[#7c3a0a] to-tinta',
  BAIXO: 'from-[#0b5a3d] to-tinta',
}

const FORM_VAZIO = { data: '', frequencia: '', nota: '', atrasos: '', atividades: '' }

/** Número do banco (6.5) -> texto do formulário em português ("6,5"). */
function paraCampo(valor) {
  return valor == null ? '' : String(valor).replace('.', ',')
}

/** Formulário para adicionar ou corrigir um lançamento do histórico. */
function FormLancamento({ inicial, editando, salvando, erro, onSalvar, onCancelar }) {
  const [campos, setCampos] = useState(inicial)
  const atualizar = (nome) => (e) => setCampos((c) => ({ ...c, [nome]: e.target.value }))

  function enviar(e) {
    e.preventDefault()
    onSalvar(campos)
  }

  return (
    <form onSubmit={enviar} className="space-y-4 border-b border-linha bg-fundo/40 px-6 py-5">
      <p className="font-bold text-tinta">{editando ? 'Corrigir lançamento' : 'Novo lançamento'}</p>
      <div className="grid gap-3 sm:grid-cols-5">
        <label className="rotulo">
          Data
          <input className="campo" type="date" required value={campos.data} onChange={atualizar('data')} />
        </label>
        <label className="rotulo">
          Frequência (%)
          <input className="campo" inputMode="decimal" required placeholder="0 a 100" value={campos.frequencia} onChange={atualizar('frequencia')} />
        </label>
        <label className="rotulo">
          Nota
          <input className="campo" inputMode="decimal" required placeholder="0 a 10" value={campos.nota} onChange={atualizar('nota')} />
        </label>
        <label className="rotulo">
          Atividades (%)
          <input className="campo" inputMode="decimal" placeholder="opcional" value={campos.atividades} onChange={atualizar('atividades')} />
        </label>
        <label className="rotulo">
          Atrasos
          <input className="campo" inputMode="numeric" placeholder="opcional" value={campos.atrasos} onChange={atualizar('atrasos')} />
        </label>
      </div>
      {erro && <p className="rounded-xl bg-risco-alerta px-3 py-2 text-sm text-risco-alto">{erro}</p>}
      <div className="flex gap-2">
        <button type="submit" disabled={salvando} className="btn-primario">
          <IconeCheck className="h-4 w-4" />
          {salvando ? 'Salvando…' : 'Salvar'}
        </button>
        <button type="button" onClick={onCancelar} className="btn-secundario">Cancelar</button>
      </div>
    </form>
  )
}

/** Uma providência: o que foi feito, por quem, e o resultado (ou o botão para concluir). */
function ItemProvidencia({ providencia, onConcluir }) {
  const [concluindo, setConcluindo] = useState(false)
  const [resultado, setResultado] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState(null)
  const emAndamento = providencia.status === 'EM_ANDAMENTO'

  async function salvar(e) {
    e.preventDefault()
    setSalvando(true)
    setErro(null)
    try {
      await onConcluir(providencia.id, resultado)
    } catch (err) {
      setErro(err.message)
      setSalvando(false)
    }
  }

  return (
    <li className={`rounded-xl border px-4 py-4 ${emAndamento ? 'border-acento/30 bg-acento/5' : 'border-linha bg-white'}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-bold text-tinta">{providencia.acao}</p>
            <span className={`chip ${emAndamento ? 'bg-acento/10 text-acento' : 'bg-emerald-50 text-risco-baixo'}`}>
              {emAndamento ? 'Em andamento' : 'Concluída'}
            </span>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Registrada por {providencia.criadaPor?.nome} em {formatarDataHora(providencia.criadaEm)}
          </p>
          {providencia.observacao && <p className="mt-2 text-sm text-slate-700">{providencia.observacao}</p>}
          {!emAndamento && (
            <div className="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-slate-700">
              <p><span className="font-semibold text-risco-baixo">Resultado:</span> {providencia.resultado}</p>
              <p className="mt-1 text-xs text-slate-500">
                Concluída por {providencia.concluidaPor?.nome} em {formatarDataHora(providencia.concluidaEm)}
              </p>
            </div>
          )}
        </div>
        {emAndamento && !concluindo && (
          <button type="button" onClick={() => setConcluindo(true)} className="btn-secundario text-xs">
            <IconeCheck className="h-4 w-4" />
            Concluir
          </button>
        )}
      </div>
      {concluindo && (
        <form onSubmit={salvar} className="mt-3 space-y-2">
          <label className="rotulo">
            O que aconteceu?
            <textarea
              className="campo min-h-[72px]"
              required
              maxLength={500}
              placeholder="Ex.: família contatada, aluno voltou a frequentar"
              value={resultado}
              onChange={(e) => setResultado(e.target.value)}
            />
          </label>
          {erro && <p className="text-sm text-risco-alto">{erro}</p>}
          <div className="flex gap-2">
            <button type="submit" disabled={salvando} className="btn-primario text-xs">
              {salvando ? 'Salvando…' : 'Concluir providência'}
            </button>
            <button type="button" onClick={() => setConcluindo(false)} className="btn-secundario text-xs">Cancelar</button>
          </div>
        </form>
      )}
    </li>
  )
}

const COR_FREQUENCIA = '#0F7B8A'
const COR_NOTA = '#B42318'

/** Linha do tempo Frequência x Notas (Tela 2 do protótipo): frequência à esquerda, nota à direita. */
function LinhaDoTempo({ dados }) {
  return (
    <ResponsiveContainer width="100%" height={280}>
      <LineChart data={dados} margin={{ top: 8, right: 4, bottom: 0, left: -8 }}>
        <CartesianGrid stroke="#E4E7EC" vertical={false} />
        <XAxis
          dataKey="mes"
          tick={{ fontSize: 12, fill: '#667085' }}
          tickLine={false}
          axisLine={{ stroke: '#D0D5DD' }}
        />
        <YAxis
          yAxisId="freq"
          domain={[0, 100]}
          tickFormatter={formatarPercentual}
          tick={{ fontSize: 12, fill: COR_FREQUENCIA }}
          tickLine={false}
          axisLine={false}
          width={48}
        />
        <YAxis
          yAxisId="nota"
          orientation="right"
          domain={[0, 10]}
          tickFormatter={formatarNota}
          tick={{ fontSize: 12, fill: COR_NOTA }}
          tickLine={false}
          axisLine={false}
          width={40}
        />
        <ReferenceLine
          yAxisId="freq"
          y={75}
          stroke="#98A2B3"
          strokeDasharray="4 4"
          label={{ value: 'frequência mínima 75%', position: 'insideBottomLeft', fontSize: 11, fill: '#667085' }}
        />
        <Tooltip
          formatter={(valor, nome) => [nome === 'Frequência' ? formatarPercentual(valor) : formatarNota(valor), nome]}
          contentStyle={{ fontSize: 12, borderRadius: 10, border: '1px solid #D9E2EC' }}
        />
        <Legend iconType="circle" wrapperStyle={{ fontSize: 12, paddingTop: 8 }} />
        <Line
          yAxisId="freq"
          name="Frequência"
          type="monotone"
          dataKey="frequencia"
          stroke={COR_FREQUENCIA}
          strokeWidth={2.5}
          dot={{ r: 4, fill: COR_FREQUENCIA, stroke: '#fff', strokeWidth: 2 }}
          activeDot={{ r: 6 }}
        />
        <Line
          yAxisId="nota"
          name="Nota"
          type="monotone"
          dataKey="nota"
          stroke={COR_NOTA}
          strokeWidth={2.5}
          dot={{ r: 4, fill: COR_NOTA, stroke: '#fff', strokeWidth: 2 }}
          activeDot={{ r: 6 }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

export default function DetalheAluno() {
  const { matricula } = useParams()
  const navigate = useNavigate()
  const [aluno, setAluno] = useState(null)
  const [erro, setErro] = useState(null)
  const [registrando, setRegistrando] = useState(null) // ação escolhida para registrar
  const [observacao, setObservacao] = useState('')
  const [salvandoProvidencia, setSalvandoProvidencia] = useState(false)
  const [erroProvidencia, setErroProvidencia] = useState(null)
  const [formulario, setFormulario] = useState(null) // { inicial, dataOriginal } ou null
  const [salvando, setSalvando] = useState(false)
  const [erroForm, setErroForm] = useState(null)
  const [excluindo, setExcluindo] = useState(null) // data do lançamento a confirmar
  const [confirmarRemocao, setConfirmarRemocao] = useState(false)
  const ehCoordenador = sessaoAtual()?.funcao === 'coordenador'

  /** Abre o formulário vazio (linha = null) ou preenchido com um lançamento para corrigir. */
  function abrirFormulario(linha) {
    setErroForm(null)
    setExcluindo(null)
    setFormulario(
      linha
        ? {
            dataOriginal: linha.data,
            inicial: {
              data: linha.data,
              frequencia: paraCampo(linha.frequencia),
              nota: paraCampo(linha.nota),
              atrasos: paraCampo(linha.atrasos),
              atividades: paraCampo(linha.atividades),
            },
          }
        : { dataOriginal: null, inicial: FORM_VAZIO },
    )
  }

  async function salvarLancamento(campos) {
    setSalvando(true)
    setErroForm(null)
    try {
      setAluno(await api.salvarLancamento(aluno.matricula, campos, formulario.dataOriginal))
      setFormulario(null)
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function excluirLancamento(data) {
    try {
      setAluno(await api.excluirLancamento(aluno.matricula, data))
      setExcluindo(null)
    } catch (e) {
      setExcluindo(null)
      setErro(e.message)
    }
  }

  function escolherAcao(acao) {
    setRegistrando(acao)
    setObservacao('')
    setErroProvidencia(null)
  }

  async function registrarProvidencia(e) {
    e.preventDefault()
    setSalvandoProvidencia(true)
    setErroProvidencia(null)
    try {
      setAluno(await api.registrarProvidencia(aluno.matricula, registrando, observacao))
      setRegistrando(null)
    } catch (err) {
      setErroProvidencia(err.message)
    } finally {
      setSalvandoProvidencia(false)
    }
  }

  // Depois de registrar/concluir, o servidor devolve o aluno inteiro atualizado e a tela redesenha
  async function concluirProvidencia(id, resultado) {
    setAluno(await api.concluirProvidencia(id, resultado))
  }

  async function removerAluno() {
    try {
      await api.removerAluno(aluno.matricula)
      navigate('/', { replace: true })
    } catch (e) {
      setErro(e.message)
    }
  }

  useEffect(() => {
    api.detalharAluno(matricula)
      .then(setAluno)
      .catch((e) => setErro(e.message))
  }, [matricula])

  if (erro) {
    return <Erro mensagem={erro} />
  }
  if (!aluno) {
    return <Carregando texto="Carregando informações do aluno…" />
  }

  const risco = aluno.risco ?? {}
  const historico = [...(aluno.historico || [])].sort((a, b) => a.data.localeCompare(b.data))
  const pontosGrafico = ultimosTresMeses(historico).map((linha) => ({
    ...linha,
    mes: formatarMesCurto(linha.data),
  }))
  const providencias = aluno.providencias ?? []
  const acoesEmAndamento = new Set(providencias.filter((p) => p.status === 'EM_ANDAMENTO').map((p) => p.acao))
  const temAtrasos = historico.some((linha) => linha.atrasos != null)
  const temAtividades = historico.some((linha) => linha.atividades != null)

  const indicadores = [
    { rotulo: 'Frequência atual', valor: formatarPercentual(risco.frequenciaAtual) },
    { rotulo: 'Nota atual', valor: formatarNota(risco.notaAtual) },
  ]
  if (risco.atividadesAtual != null) {
    indicadores.push({ rotulo: 'Atividades entregues', valor: formatarPercentual(risco.atividadesAtual) })
  }
  if (risco.atrasosAtual != null) {
    indicadores.push({ rotulo: 'Atrasos no período', valor: String(risco.atrasosAtual) })
  }

  return (
    <section className="space-y-6">
      <Link to="/" className="inline-flex items-center gap-1 text-sm font-semibold text-slate-500 hover:text-tinta">
        <IconeVoltar className="h-4 w-4" />
        Voltar para a lista
      </Link>

      <div className="painel overflow-hidden">
        <div className={`bg-gradient-to-br ${CORES_TOPO[risco.nivel] ?? 'from-tinta to-tinta'} px-6 py-6 text-white`}>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight">{aluno.nome}</h1>
              <p className="mt-1 text-sm text-white/75">
                Matrícula {aluno.matricula} · Turma {aluno.turma}
              </p>
            </div>
            <div className="rounded-2xl bg-white px-4 py-3 text-right">
              <BadgeRisco nivel={risco.nivel} grande />
              <p className="mt-1.5 text-xs font-semibold text-slate-500">{NIVEIS[risco.nivel]?.descricao}</p>
            </div>
          </div>
        </div>
        <div className="grid gap-4 p-6 sm:grid-cols-2 lg:grid-cols-5">
          {indicadores.map((item) => (
            <div key={item.rotulo}>
              <p className="kpi-rotulo">{item.rotulo}</p>
              <p className="kpi-valor">{item.valor}</p>
            </div>
          ))}
          <div>
            <p className="kpi-rotulo">Atualizado em</p>
            <p className="mt-2 text-sm font-bold">{formatarDataHora(risco.calculadoEm)}</p>
          </div>
        </div>
      </div>

      <div className="painel p-6">
        <h2 className="text-lg font-extrabold">Linha do tempo — Frequência x Notas</h2>
        <p className="mt-1 text-sm text-slate-500">Últimos 3 meses.</p>
        <div className="mt-5">
          <LinhaDoTempo dados={pontosGrafico} />
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="painel p-6">
          <h2 className="text-lg font-extrabold">Sinais identificados</h2>
          {risco.sinais?.length ? (
            <ul className="mt-4 space-y-2">
              {risco.sinais.map((sinal) => (
                <li key={sinal} className="flex gap-3 rounded-xl border border-linha bg-fundo/60 px-4 py-3 text-sm">
                  <IconeAtencao className="mt-0.5 h-4 w-4 shrink-0 text-risco-medio" />
                  {sinal}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-4 flex items-center gap-2 text-sm text-risco-baixo">
              <IconeCheck className="h-5 w-5" />
              Nenhum sinal preocupante.
            </p>
          )}
        </div>

        <div className="painel p-6">
          <h2 className="text-lg font-extrabold">Ações recomendadas</h2>
          <p className="mt-1 text-sm text-slate-500">
            Clique para registrar que a escola vai tomar essa providência.
          </p>
          <div className="mt-4 flex flex-wrap gap-2">
            {risco.acoes?.map((acao) =>
              acoesEmAndamento.has(acao) ? (
                <span key={acao} className="inline-flex items-center gap-1.5 rounded-[0.625rem] bg-acento/10 px-4 py-2.5 text-sm font-bold text-acento">
                  <IconeCheck className="h-4 w-4" />
                  {acao} · em andamento
                </span>
              ) : (
                <button
                  key={acao}
                  type="button"
                  onClick={() => escolherAcao(acao)}
                  className={registrando === acao ? 'btn-primario bg-acento' : 'btn-secundario'}
                >
                  {acao}
                </button>
              ),
            )}
          </div>
          {registrando && (
            <form onSubmit={registrarProvidencia} className="mt-4 space-y-3 rounded-xl border border-linha bg-fundo/50 p-4">
              <p className="text-sm font-bold text-tinta">Registrar: {registrando}</p>
              <label className="rotulo">
                Observação (opcional)
                <textarea
                  className="campo min-h-[72px]"
                  maxLength={500}
                  placeholder="Ex.: aluno relatou problema de transporte"
                  value={observacao}
                  onChange={(e) => setObservacao(e.target.value)}
                />
              </label>
              {erroProvidencia && <p className="text-sm text-risco-alto">{erroProvidencia}</p>}
              <div className="flex gap-2">
                <button type="submit" disabled={salvandoProvidencia} className="btn-primario text-xs">
                  <IconeCheck className="h-4 w-4" />
                  {salvandoProvidencia ? 'Registrando…' : 'Registrar providência'}
                </button>
                <button type="button" onClick={() => setRegistrando(null)} className="btn-secundario text-xs">Cancelar</button>
              </div>
            </form>
          )}
        </div>
      </div>

      <div className="painel p-6">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="text-lg font-extrabold">Providências</h2>
          {providencias.length > 0 && (
            <p className="text-xs text-slate-500">
              {providencias.filter((p) => p.status === 'EM_ANDAMENTO').length} em andamento ·{' '}
              {providencias.filter((p) => p.status === 'CONCLUIDA').length} concluída(s)
            </p>
          )}
        </div>
        {providencias.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">
            {risco.nivel === 'ALTO'
              ? 'Nenhuma providência registrada ainda para este aluno em risco alto. Use as ações recomendadas acima.'
              : 'Nenhuma providência registrada ainda.'}
          </p>
        ) : (
          <ul className="mt-4 space-y-3">
            {providencias.map((p) => (
              <ItemProvidencia key={p.id} providencia={p} onConcluir={concluirProvidencia} />
            ))}
          </ul>
        )}
      </div>

      <div className="painel overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-linha px-6 py-4">
          <h2 className="text-lg font-extrabold">Histórico completo</h2>
          {!formulario && (
            <button type="button" onClick={() => abrirFormulario(null)} className="btn-secundario text-xs">
              + Adicionar lançamento
            </button>
          )}
        </div>
        {formulario && (
          <FormLancamento
            key={formulario.dataOriginal ?? 'novo'}
            inicial={formulario.inicial}
            editando={Boolean(formulario.dataOriginal)}
            salvando={salvando}
            erro={erroForm}
            onSalvar={salvarLancamento}
            onCancelar={() => setFormulario(null)}
          />
        )}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[420px] text-left text-sm">
            <thead className="bg-fundo/60 text-xs text-slate-500">
              <tr>
                <th className="px-6 py-3 font-semibold">Data</th>
                <th className="px-6 py-3 text-right font-semibold">Frequência</th>
                <th className="px-6 py-3 text-right font-semibold">Nota</th>
                {temAtividades && <th className="px-6 py-3 text-right font-semibold">Atividades entregues</th>}
                {temAtrasos && <th className="px-6 py-3 text-right font-semibold">Atrasos</th>}
                <th className="px-6 py-3 text-right font-semibold"><span className="sr-only">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {[...historico].reverse().map((linha) => (
                <tr key={linha.data} className="border-t border-linha">
                  <td className="px-6 py-3">{formatarData(linha.data)}</td>
                  <td className="px-6 py-3 text-right tabular-nums">{formatarPercentual(linha.frequencia)}</td>
                  <td className="px-6 py-3 text-right tabular-nums">{formatarNota(linha.nota)}</td>
                  {temAtividades && (
                    <td className="px-6 py-3 text-right tabular-nums">{formatarPercentual(linha.atividades)}</td>
                  )}
                  {temAtrasos && (
                    <td className="px-6 py-3 text-right tabular-nums">{linha.atrasos ?? '—'}</td>
                  )}
                  <td className="whitespace-nowrap px-6 py-3 text-right text-xs">
                    {excluindo === linha.data ? (
                      <span className="inline-flex items-center gap-2">
                        <span className="text-slate-600">Excluir este lançamento?</span>
                        <button type="button" onClick={() => excluirLancamento(linha.data)} className="font-bold text-risco-alto hover:underline">
                          Sim, excluir
                        </button>
                        <button type="button" onClick={() => setExcluindo(null)} className="font-semibold text-slate-500 hover:underline">
                          Não
                        </button>
                      </span>
                    ) : (
                      <span className="inline-flex gap-3">
                        <button type="button" onClick={() => abrirFormulario(linha)} className="font-semibold text-acento hover:underline">
                          Corrigir
                        </button>
                        <button type="button" onClick={() => setExcluindo(linha.data)} className="font-semibold text-slate-500 hover:text-risco-alto hover:underline">
                          Excluir
                        </button>
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {ehCoordenador && (
        <div className="painel flex flex-wrap items-center justify-between gap-4 border-risco-alto/20 p-6">
          <div className="max-w-xl">
            <h2 className="font-extrabold">Remover aluno</h2>
            <p className="mt-1 text-sm text-slate-500">
              Use se o aluno saiu da escola ou foi cadastrado por engano. O histórico e os avisos dele serão apagados.
            </p>
          </div>
          {confirmarRemocao ? (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm font-semibold text-risco-alto">Tem certeza? Não dá para desfazer.</span>
              <button type="button" onClick={removerAluno} className="btn-primario bg-risco-alto">Sim, remover</button>
              <button type="button" onClick={() => setConfirmarRemocao(false)} className="btn-secundario">Cancelar</button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setConfirmarRemocao(true)}
              className="btn-secundario text-risco-alto hover:border-risco-alto"
            >
              Remover aluno
            </button>
          )}
        </div>
      )}
    </section>
  )
}
