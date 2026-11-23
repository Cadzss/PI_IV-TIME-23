// Autor: Breno Rogeri Miquelino
// Alterações: Lucas Canto — textos simples, quadros "O que cada risco significa"/"O que levamos em conta",
//             contador "Risco alto sem providência", filtro de providência, boas-vindas no primeiro acesso
//             e correção da lupa da busca
//
// Tela 1 do protótipo: lista de alunos ordenada por risco, com filtros e indicadores no topo.
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import BadgeRisco from '../components/BadgeRisco.jsx'
import { Carregando, Erro } from '../components/Estado.jsx'
import { IconeArquivo, IconeBusca, IconeEnviar, IconeLogo, IconePainel } from '../components/Icones.jsx'
import { baixarModelo } from '../planilha.js'
import { formatarDataHora, formatarNota, formatarPercentual, NIVEIS } from '../formatos.js'

/** Média simples de um campo do risco (ex.: frequenciaAtual) entre os alunos da lista. */
function media(lista, campo) {
  if (!lista?.length) return null
  const soma = lista.reduce((acc, a) => acc + (Number(a.risco?.[campo]) || 0), 0)
  return soma / lista.length
}

const CRITERIOS = [
  'Frequência abaixo de 75% (o mínimo exigido)',
  'Frequência abaixo de 85%',
  'Nota abaixo de 5',
  'Nota abaixo de 7',
  'Notas caindo nos últimos meses',
  'Frequência caindo nos últimos meses',
  'Atrasos recorrentes (5 ou mais no período)',
  'Poucas atividades entregues (menos de 75%)',
]

/** Primeiro acesso: ainda não há nenhum aluno no sistema. */
function BoasVindas() {
  const [baixando, setBaixando] = useState(false)

  async function baixar() {
    setBaixando(true)
    try {
      await baixarModelo()
    } finally {
      setBaixando(false)
    }
  }

  const passos = [
    { Icone: IconeArquivo, titulo: 'Baixe a planilha modelo', texto: 'Ela já vem com as colunas certas e um exemplo.' },
    { Icone: IconeEnviar, titulo: 'Preencha e envie', texto: 'Notas e frequência dos alunos, direto do Excel.' },
    { Icone: IconePainel, titulo: 'Acompanhe aqui', texto: 'Os alunos aparecem do maior para o menor risco.' },
  ]

  return (
    <section className="mx-auto max-w-3xl py-6">
      <div className="painel overflow-hidden">
        <div className="bg-gradient-to-br from-[#1d4e6e] to-tinta px-8 py-10 text-center text-white">
          <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-white/10 text-teal-300">
            <IconeLogo className="h-8 w-8" />
          </span>
          <h1 className="mt-4 text-3xl font-extrabold tracking-tight">Bem-vindo ao Early-Warning</h1>
          <p className="mx-auto mt-2 max-w-md text-sm text-white/75">
            Ainda não há alunos no sistema. Envie a primeira planilha para descobrir quem precisa de atenção.
          </p>
        </div>
        <ol className="grid gap-4 p-6 sm:grid-cols-3">
          {passos.map(({ Icone, titulo, texto }, i) => (
            <li key={titulo} className="rounded-xl border border-linha bg-fundo/50 p-4">
              <div className="flex items-center gap-2">
                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-tinta text-xs font-extrabold text-white">
                  {i + 1}
                </span>
                <Icone className="h-5 w-5 text-acento" />
              </div>
              <p className="mt-3 font-bold text-tinta">{titulo}</p>
              <p className="mt-1 text-sm text-slate-500">{texto}</p>
            </li>
          ))}
        </ol>
        <div className="flex flex-wrap justify-center gap-3 border-t border-linha p-6">
          <button type="button" onClick={baixar} disabled={baixando} className="btn-secundario">
            <IconeArquivo className="h-4 w-4" />
            {baixando ? 'Gerando…' : 'Baixar planilha modelo'}
          </button>
          <Link to="/importar" className="btn-primario">
            <IconeEnviar className="h-4 w-4" />
            Enviar planilha
          </Link>
        </div>
      </div>
    </section>
  )
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [turmas, setTurmas] = useState([])
  const [turma, setTurma] = useState('')
  const [nivel, setNivel] = useState('')
  const [busca, setBusca] = useState('')
  const [acompanhamento, setAcompanhamento] = useState('')
  const [alunos, setAlunos] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    api.listarTurmas().then(setTurmas).catch((e) => setErro(e.message))
  }, [])

  // Toda vez que muda a turma ou o nível, pede a lista de novo ao servidor (o filtro é feito lá)
  useEffect(() => {
    setAlunos(null)
    api.listarAlunos({ turma, nivel }).then((lista) => {
      setAlunos(lista)
      setErro(null)
    }).catch((e) => setErro(e.message))
  }, [turma, nivel])

  // Busca por nome/matrícula e filtro de providência são feitos aqui no navegador, em cima da lista
  // que já chegou (useMemo só refaz a conta quando alunos/busca/acompanhamento mudam)
  const filtrados = useMemo(() => {
    if (!alunos) return null
    const q = busca.trim().toLowerCase()
    return alunos.filter((a) => {
      if (acompanhamento === 'sem' && a.emAcompanhamento) return false
      if (acompanhamento === 'com' && !a.emAcompanhamento) return false
      return !q || a.nome?.toLowerCase().includes(q) || String(a.matricula).toLowerCase().includes(q)
    })
  }, [alunos, busca, acompanhamento])

  const contagem = (n) => filtrados?.filter((a) => a.risco?.nivel === n).length ?? 0
  const mediaFreq = media(filtrados, 'frequenciaAtual')
  const mediaNota = media(filtrados, 'notaAtual')
  const atualizadoEm = filtrados?.[0]?.risco?.calculadoEm
  const altoSemProvidencia = filtrados?.filter((a) => a.risco?.nivel === 'ALTO' && !a.emAcompanhamento).length ?? 0

  function verSemProvidencia() {
    setNivel('ALTO')
    setAcompanhamento('sem')
  }

  // Sem nenhum aluno cadastrado (e sem filtro aplicado): mostra o passo a passo do primeiro acesso
  if (alunos && alunos.length === 0 && !turma && !nivel && !erro) {
    return <BoasVindas />
  }

  function abrir(matricula) {
    navigate(`/alunos/${encodeURIComponent(matricula)}`)
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="titulo">Alunos que precisam de atenção</h1>
          <p className="subtitulo">Quem tem mais chance de deixar a escola aparece primeiro.</p>
        </div>
        {atualizadoEm && (
          <p className="text-xs text-slate-500">Atualizado em {formatarDataHora(atualizadoEm)}</p>
        )}
      </div>

      {filtrados && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          <div className="kpi">
            <p className="kpi-rotulo">Alunos</p>
            <p className="kpi-valor">{filtrados.length}</p>
          </div>
          <div className="kpi border-l-4 border-l-[#B42318]">
            <p className="kpi-rotulo">Risco alto</p>
            <p className="kpi-valor text-risco-alto">{contagem('ALTO')}</p>
          </div>
          <div className="kpi border-l-4 border-l-[#B54708]">
            <p className="kpi-rotulo">Risco médio</p>
            <p className="kpi-valor text-risco-medio">{contagem('MEDIO')}</p>
          </div>
          <button
            type="button"
            onClick={verSemProvidencia}
            title="Mostrar só estes alunos"
            className={`kpi text-left transition hover:shadow-md ${
              altoSemProvidencia > 0 ? 'border-risco-alto/40 bg-risco-alerta' : ''
            }`}
          >
            <p className="kpi-rotulo">Risco alto sem providência</p>
            <p className={`kpi-valor ${altoSemProvidencia > 0 ? 'text-risco-alto' : 'text-risco-baixo'}`}>{altoSemProvidencia}</p>
          </button>
          <div className="kpi">
            <p className="kpi-rotulo">Frequência média</p>
            <p className="kpi-valor">{formatarPercentual(mediaFreq)}</p>
          </div>
          <div className="kpi">
            <p className="kpi-rotulo">Nota média</p>
            <p className="kpi-valor">{formatarNota(mediaNota)}</p>
          </div>
        </div>
      )}

      <div className="painel grid gap-3 p-4 md:grid-cols-5">
        <label className="rotulo md:col-span-2">
          Buscar aluno
          <span className="relative">
            <IconeBusca className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              className="campo"
              style={{ paddingLeft: '2.25rem' }}
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              placeholder="Nome ou matrícula"
            />
          </span>
        </label>
        <label className="rotulo">
          Turma
          <select className="campo" value={turma} onChange={(e) => setTurma(e.target.value)}>
            <option value="">Todas as turmas</option>
            {turmas.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label className="rotulo">
          Risco
          <select className="campo" value={nivel} onChange={(e) => setNivel(e.target.value)}>
            <option value="">Todos</option>
            <option value="ALTO">Alto</option>
            <option value="MEDIO">Médio</option>
            <option value="BAIXO">Baixo</option>
          </select>
        </label>
        <label className="rotulo">
          Providência
          <select className="campo" value={acompanhamento} onChange={(e) => setAcompanhamento(e.target.value)}>
            <option value="">Todos</option>
            <option value="sem">Sem providência</option>
            <option value="com">Em acompanhamento</option>
          </select>
        </label>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
        <div>
          {erro && <Erro mensagem={erro} />}
          {!erro && !alunos && <Carregando texto="Carregando alunos…" />}
          {filtrados && (
            <div className="painel overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] text-left text-sm">
                  <thead className="border-b border-linha bg-fundo/60 text-xs text-slate-500">
                    <tr>
                      <th className="px-5 py-3 font-semibold">Aluno</th>
                      <th className="px-4 py-3 font-semibold">Turma</th>
                      <th className="px-4 py-3 text-right font-semibold">Frequência</th>
                      <th className="px-4 py-3 text-right font-semibold">Nota</th>
                      <th className="px-4 py-3 font-semibold">Risco</th>
                      <th className="px-5 py-3 font-semibold">O que chama atenção</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtrados.length === 0 && (
                      <tr>
                        <td colSpan={6} className="px-5 py-14 text-center text-slate-500">
                          Nenhum aluno encontrado com esses filtros.
                        </td>
                      </tr>
                    )}
                    {filtrados.map((a) => (
                      <tr
                        key={a.matricula}
                        onClick={() => abrir(a.matricula)}
                        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); abrir(a.matricula) } }}
                        role="link"
                        tabIndex={0}
                        className="cursor-pointer border-b border-linha transition last:border-0 hover:bg-fundo/70 focus:bg-fundo/70 focus:outline-none"
                      >
                        <td className="px-5 py-3.5">
                          <p className="font-bold text-tinta">{a.nome}</p>
                          <p className="text-xs text-slate-500">Matrícula {a.matricula}</p>
                        </td>
                        <td className="px-4 py-3.5 text-slate-700">{a.turma}</td>
                        <td className="px-4 py-3.5 text-right tabular-nums">{formatarPercentual(a.risco?.frequenciaAtual)}</td>
                        <td className="px-4 py-3.5 text-right tabular-nums">{formatarNota(a.risco?.notaAtual)}</td>
                        <td className="px-4 py-3.5">
                          <BadgeRisco nivel={a.risco?.nivel} />
                          {a.emAcompanhamento && (
                            <p className="mt-1 text-[11px] font-semibold text-acento">Em acompanhamento</p>
                          )}
                        </td>
                        <td className="max-w-[260px] truncate px-5 py-3.5 text-xs text-slate-600">
                          {a.risco?.sinais?.[0] || 'Nada preocupante'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
          {filtrados?.length > 0 && (
            <p className="mt-3 text-xs text-slate-500">Clique em um aluno para ver o histórico e o que fazer.</p>
          )}
        </div>

        <aside className="space-y-4">
          <div className="painel p-5">
            <h2 className="text-sm font-extrabold">O que cada risco significa</h2>
            <ul className="mt-4 space-y-3">
              {['ALTO', 'MEDIO', 'BAIXO'].map((n) => (
                <li key={n} className="flex items-center justify-between gap-3">
                  <BadgeRisco nivel={n} />
                  <span className="text-right text-xs text-slate-600">{NIVEIS[n].descricao}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="painel p-5">
            <h2 className="text-sm font-extrabold">O que levamos em conta</h2>
            <ul className="mt-3 space-y-2 text-xs leading-relaxed text-slate-600">
              {CRITERIOS.map((c) => (
                <li key={c} className="flex gap-2">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-acento" />
                  {c}
                </li>
              ))}
            </ul>
            <p className="mt-4 text-xs leading-relaxed text-slate-500">
              Frequência abaixo do mínimo já coloca o aluno em risco alto. Os outros sinais vão se somando.
            </p>
          </div>
        </aside>
      </div>
    </section>
  )
}
