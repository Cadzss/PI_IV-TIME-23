// Autor: Cadu Spadari
// Alterações: Lucas Canto — aceita Excel (.xlsx), planilha modelo, prévia antes de gravar (3 passos),
//             colunas opcionais de engajamento e textos simples
//
// Tela de enviar planilha: escolher arquivo -> conferir a prévia -> confirmar e salvar.
import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { Erro } from '../components/Estado.jsx'
import { IconeArquivo, IconeAtencao, IconeAtualizar, IconeCheck, IconeEnviar } from '../components/Icones.jsx'
import { formatarData, formatarNota, formatarPercentual } from '../formatos.js'
import { baixarModelo, lerPlanilha } from '../planilha.js'

const COLUNAS = [
  { nome: 'matricula', exemplo: '2026001', dica: 'Matrícula do aluno' },
  { nome: 'nome', exemplo: 'João Pereira', dica: 'Nome do aluno' },
  { nome: 'turma', exemplo: '8ºA', dica: 'Turma' },
  { nome: 'data', exemplo: '01/11/2026', dica: 'Dia/mês/ano' },
  { nome: 'frequencia', exemplo: '62', dica: 'De 0 a 100' },
  { nome: 'nota', exemplo: '5,2', dica: 'De 0 a 10' },
  { nome: 'atrasos', exemplo: '6', dica: 'Opcional: atrasos no período', opcional: true },
  { nome: 'atividades', exemplo: '47', dica: 'Opcional: % entregues', opcional: true },
]

function Passo({ numero, titulo, ativo, feito }) {
  return (
    <div className="flex items-center gap-2">
      <span
        className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-extrabold ${
          feito ? 'bg-risco-baixo text-white' : ativo ? 'bg-tinta text-white' : 'bg-linha text-slate-500'
        }`}
      >
        {feito ? <IconeCheck className="h-4 w-4" /> : numero}
      </span>
      <span className={`text-sm font-semibold ${ativo || feito ? 'text-tinta' : 'text-slate-400'}`}>{titulo}</span>
    </div>
  )
}

function Contador({ valor, rotulo, classe = 'text-tinta' }) {
  return (
    <div className="kpi">
      <p className="kpi-rotulo">{rotulo}</p>
      <p className={`kpi-valor ${classe}`}>{valor}</p>
    </div>
  )
}

export default function Importar() {
  const entradaArquivo = useRef(null)
  const [arquivo, setArquivo] = useState(null)
  const [conteudo, setConteudo] = useState(null)
  const [previa, setPrevia] = useState(null)
  const [resultado, setResultado] = useState(null)
  const [lendo, setLendo] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [baixando, setBaixando] = useState(false)
  const [recalculando, setRecalculando] = useState(false)
  const [recalculo, setRecalculo] = useState(null)
  const [erro, setErro] = useState(null)

  // Em que passo estamos: 1 = escolher, 2 = conferir a prévia, 3 = pronto
  const etapa = resultado ? 3 : previa ? 2 : 1

  function recomecar() {
    setArquivo(null)
    setConteudo(null)
    setPrevia(null)
    setResultado(null)
    setErro(null)
    if (entradaArquivo.current) entradaArquivo.current.value = ''
  }

  // Ao escolher o arquivo já lê e pede a prévia ao servidor (nada é gravado nessa hora)
  async function aoEscolherArquivo(e) {
    const escolhido = e.target.files[0]
    if (!escolhido) return
    recomecar()
    setArquivo(escolhido)
    setLendo(true)
    try {
      const texto = await lerPlanilha(escolhido)
      setConteudo(texto)
      setPrevia(await api.previsualizarCsv(texto))
    } catch (err) {
      setErro(err.message)
    } finally {
      setLendo(false)
    }
  }

  async function aoConfirmar() {
    setEnviando(true)
    setErro(null)
    try {
      setResultado(await api.importarCsv(conteudo))
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
    }
  }

  async function aoBaixarModelo() {
    setBaixando(true)
    try {
      await baixarModelo()
    } catch {
      setErro('Não foi possível gerar a planilha modelo. Tente novamente.')
    } finally {
      setBaixando(false)
    }
  }

  async function aoRecalcular() {
    setRecalculando(true)
    setErro(null)
    setRecalculo(null)
    try {
      setRecalculo(await api.recalcularRiscos())
    } catch (err) {
      setErro(err.message)
    } finally {
      setRecalculando(false)
    }
  }

  return (
    <section className="mx-auto max-w-4xl space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="titulo">Enviar planilha</h1>
          <p className="subtitulo">Envie as notas e a frequência dos alunos direto do Excel. Você confere tudo antes de salvar.</p>
        </div>
        <button type="button" onClick={aoBaixarModelo} disabled={baixando} className="btn-secundario">
          <IconeArquivo className="h-4 w-4" />
          {baixando ? 'Gerando…' : 'Baixar planilha modelo'}
        </button>
      </div>

      <div className="painel flex flex-wrap items-center gap-x-8 gap-y-3 px-6 py-4">
        <Passo numero={1} titulo="Escolher arquivo" ativo={etapa === 1} feito={etapa > 1} />
        <Passo numero={2} titulo="Conferir" ativo={etapa === 2} feito={etapa > 2} />
        <Passo numero={3} titulo="Pronto" ativo={false} feito={etapa === 3} />
      </div>

      {etapa === 1 && (
        <div className="painel p-6">
          <label
            className={`flex cursor-pointer flex-col items-center gap-3 rounded-2xl border-2 border-dashed px-6 py-12 text-center transition ${
              lendo ? 'border-acento bg-acento/5' : 'border-linha hover:border-acento/60 hover:bg-fundo/60'
            }`}
          >
            <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-acento/10 text-acento">
              {lendo ? (
                <span className="h-6 w-6 animate-spin rounded-full border-2 border-acento/30 border-t-acento" />
              ) : (
                <IconeEnviar className="h-7 w-7" />
              )}
            </span>
            {lendo ? (
              <span className="font-bold text-tinta">Lendo {arquivo?.name}…</span>
            ) : (
              <>
                <span className="text-lg font-bold text-tinta">Clique para escolher a planilha</span>
                <span className="text-sm text-slate-500">Arquivo do Excel (.xlsx) ou .csv</span>
              </>
            )}
            <input
              ref={entradaArquivo}
              type="file"
              accept=".xlsx,.csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/csv"
              onChange={aoEscolherArquivo}
              disabled={lendo}
              className="sr-only"
            />
          </label>
        </div>
      )}

      {erro && <Erro mensagem={erro} />}

      {etapa === 2 && previa && (
        <div className="space-y-4">
          <div className="painel flex flex-wrap items-center justify-between gap-3 px-6 py-4">
            <p className="flex items-center gap-2 font-bold text-tinta">
              <IconeArquivo className="h-5 w-5 text-acento" />
              {arquivo?.name}
            </p>
            <button type="button" onClick={recomecar} className="text-sm font-semibold text-acento hover:underline">
              Escolher outro arquivo
            </button>
          </div>

          <div className="grid gap-3 sm:grid-cols-4">
            <Contador valor={previa.registros} rotulo="Lançamentos válidos" />
            <Contador valor={previa.novos} rotulo="Alunos novos" classe="text-acento" />
            <Contador valor={previa.existentes} rotulo="Alunos já cadastrados" />
            <Contador
              valor={previa.erros.length}
              rotulo="Linhas com problema"
              classe={previa.erros.length ? 'text-risco-alto' : 'text-risco-baixo'}
            />
          </div>

          {previa.erros.length > 0 && (
            <div className="rounded-2xl border border-risco-alto/20 bg-risco-alerta px-5 py-4 text-sm text-risco-alto">
              <p className="font-bold">Estas linhas têm problema e vão ficar de fora:</p>
              <ul className="mt-2 list-inside list-disc space-y-1">
                {previa.erros.slice(0, 15).map((msg) => <li key={msg}>{msg}</li>)}
              </ul>
              {previa.erros.length > 15 && <p className="mt-2">…e mais {previa.erros.length - 15}.</p>}
              <p className="mt-3 text-xs">Você pode corrigir a planilha e escolher de novo, ou seguir só com as linhas válidas.</p>
            </div>
          )}

          {previa.avisos.length > 0 && (
            <div className="rounded-2xl bg-amber-50 px-5 py-4 text-sm text-risco-medio">
              <p className="font-bold">Vale conferir:</p>
              <ul className="mt-2 list-inside list-disc space-y-1">
                {previa.avisos.map((msg) => <li key={msg}>{msg}</li>)}
              </ul>
            </div>
          )}

          {previa.amostra.length > 0 && (
            <div className="painel overflow-hidden">
              <div className="border-b border-linha px-6 py-3">
                <h2 className="font-extrabold">Primeiros lançamentos</h2>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[640px] text-left text-sm">
                  <thead className="bg-fundo/60 text-xs text-slate-500">
                    <tr>
                      <th className="px-5 py-2.5 font-semibold">Aluno</th>
                      <th className="px-4 py-2.5 font-semibold">Turma</th>
                      <th className="px-4 py-2.5 font-semibold">Data</th>
                      <th className="px-4 py-2.5 text-right font-semibold">Frequência</th>
                      <th className="px-4 py-2.5 text-right font-semibold">Nota</th>
                      <th className="px-4 py-2.5 text-right font-semibold">Atividades</th>
                      <th className="px-4 py-2.5 text-right font-semibold">Atrasos</th>
                    </tr>
                  </thead>
                  <tbody>
                    {previa.amostra.map((item) => (
                      <tr key={`${item.matricula}-${item.registro.data}`} className="border-t border-linha">
                        <td className="px-5 py-2.5">
                          <span className="font-semibold">{item.nome}</span>
                          {item.novo && <span className="chip ml-2 bg-acento/10 text-acento">Novo</span>}
                        </td>
                        <td className="px-4 py-2.5">{item.turma}</td>
                        <td className="px-4 py-2.5">{formatarData(item.registro.data)}</td>
                        <td className="px-4 py-2.5 text-right tabular-nums">{formatarPercentual(item.registro.frequencia)}</td>
                        <td className="px-4 py-2.5 text-right tabular-nums">{formatarNota(item.registro.nota)}</td>
                        <td className="px-4 py-2.5 text-right tabular-nums">
                          {item.registro.atividades == null ? '—' : formatarPercentual(item.registro.atividades)}
                        </td>
                        <td className="px-4 py-2.5 text-right tabular-nums">{item.registro.atrasos ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {previa.registros > previa.amostra.length && (
                <p className="border-t border-linha px-6 py-3 text-xs text-slate-500">
                  Mostrando {previa.amostra.length} de {previa.registros} lançamentos.
                </p>
              )}
            </div>
          )}

          <div className="painel flex flex-wrap items-center justify-between gap-4 p-5">
            <p className="text-sm text-slate-600">
              {previa.registros > 0
                ? <>Ao confirmar, <strong>{previa.registros}</strong> lançamento(s) serão salvos e a situação dos alunos será atualizada.</>
                : 'Nenhuma linha válida para salvar. Corrija a planilha e escolha de novo.'}
            </p>
            <div className="flex gap-2">
              <button type="button" onClick={recomecar} className="btn-secundario">Cancelar</button>
              <button type="button" onClick={aoConfirmar} disabled={enviando || previa.registros === 0} className="btn-primario">
                <IconeCheck className="h-4 w-4" />
                {enviando ? 'Salvando…' : 'Confirmar e salvar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {etapa === 3 && resultado && (
        <div className="painel space-y-4 p-6">
          <p className="flex items-center gap-2 text-lg font-bold text-risco-baixo">
            <IconeCheck className="h-6 w-6" />
            Pronto! {resultado.alunos} aluno(s) atualizado(s), com {resultado.registros} lançamento(s).
          </p>
          <div className="flex flex-wrap gap-2">
            <Link to="/" className="btn-primario">Ver a lista de alunos</Link>
            <button type="button" onClick={recomecar} className="btn-secundario">Enviar outra planilha</button>
          </div>
        </div>
      )}

      {etapa === 1 && (
        <div className="painel p-6">
          <h2 className="text-lg font-extrabold">Como montar a planilha</h2>
          <p className="mt-1 text-sm text-slate-500">
            O jeito mais fácil é <button type="button" onClick={aoBaixarModelo} className="font-bold text-acento hover:underline">baixar a planilha modelo</button> e
            preencher. A primeira linha precisa ter estes nomes de coluna:
          </p>
          <div className="mt-4 overflow-x-auto rounded-xl border border-linha">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="bg-fundo/60 text-xs">
                <tr>
                  {COLUNAS.map((c) => (
                    <th key={c.nome} className={`px-4 py-2.5 font-bold ${c.opcional ? 'text-slate-400' : 'text-tinta'}`}>
                      {c.nome}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                <tr className="border-t border-linha">
                  {COLUNAS.map((c) => (
                    <td key={c.nome} className="px-4 py-2.5 text-slate-700">{c.exemplo}</td>
                  ))}
                </tr>
                <tr className="border-t border-linha bg-fundo/30">
                  {COLUNAS.map((c) => (
                    <td key={c.nome} className="px-4 py-2 text-xs text-slate-500">{c.dica}</td>
                  ))}
                </tr>
              </tbody>
            </table>
          </div>
          <ul className="mt-4 space-y-2 text-xs text-slate-500">
            <li className="flex items-start gap-2">
              <IconeCheck className="mt-0.5 h-4 w-4 shrink-0" />
              As colunas atrasos e atividades são opcionais. Quando vierem, o engajamento do aluno também entra na avaliação.
            </li>
            <li className="flex items-start gap-2">
              <IconeAtencao className="mt-0.5 h-4 w-4 shrink-0" />
              Se o aluno já tiver um lançamento na mesma data, o novo substitui o antigo.
            </li>
          </ul>
        </div>
      )}

      <div className="painel flex flex-wrap items-center justify-between gap-4 p-6">
        <div className="max-w-md">
          <h2 className="font-extrabold">Atualizar a situação de todos os alunos</h2>
          <p className="mt-1 text-sm text-slate-500">
            Use quando os critérios de risco mudarem e você quiser reavaliar todo mundo.
          </p>
          {recalculo && (
            <p className="mt-2 flex items-center gap-2 text-sm font-semibold text-risco-baixo">
              <IconeCheck className="h-4 w-4" />
              {recalculo.recalculados} aluno(s) reavaliado(s).
            </p>
          )}
        </div>
        <button type="button" disabled={recalculando} onClick={aoRecalcular} className="btn-secundario">
          <IconeAtualizar className={`h-4 w-4 ${recalculando ? 'animate-spin' : ''}`} />
          {recalculando ? 'Atualizando…' : 'Atualizar agora'}
        </button>
      </div>
    </section>
  )
}
