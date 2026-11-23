// Autor: Cadu Spadari
import { useState } from 'react'
import { api } from '../api.js'
import { Erro } from '../components/Estado.jsx'

const EXEMPLO_CSV = `matricula,nome,turma,data,frequencia,nota
2026001,João P.,8ºA,2026-11-01,62,5.2`

export default function Importar() {
  const [arquivo, setArquivo] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [recalculando, setRecalculando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [recalculo, setRecalculo] = useState(null)
  const [erro, setErro] = useState(null)

  function aoEscolherArquivo(e) {
    const selecionado = e.target.files[0] ?? null
    setArquivo(selecionado)
  }

  async function aoImportar(e) {
    e.preventDefault()
    if (!arquivo) return

    setEnviando(true)
    setErro(null)
    setResultado(null)
    try {
      const texto = await arquivo.text()
      setResultado(await api.importarCsv(texto))
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
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
    <section className="mx-auto max-w-3xl space-y-6">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-acento">Entrada de dados</p>
        <h1 className="titulo">Importar planilha CSV</h1>
        <p className="subtitulo">O arquivo vai para o servidor, que valida, grava no MongoDB e recalcula o risco.</p>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="kpi">
          <p className="text-xs font-bold uppercase text-slate-500">Passo 1</p>
          <p className="mt-1 font-bold">Escolher CSV</p>
        </div>
        <div className="kpi">
          <p className="text-xs font-bold uppercase text-slate-500">Passo 2</p>
          <p className="mt-1 font-bold">Importar no servidor</p>
        </div>
        <div className="kpi">
          <p className="text-xs font-bold uppercase text-slate-500">Passo 3</p>
          <p className="mt-1 font-bold">Ver painel atualizado</p>
        </div>
      </div>

      <form onSubmit={aoImportar} className="painel space-y-4 p-6">
        <label className="rotulo">
          Arquivo
          <input type="file" accept=".csv,text/csv" onChange={aoEscolherArquivo} className="campo" />
        </label>
        {arquivo && <p className="text-xs text-slate-500">Selecionado: {arquivo.name}</p>}
        <div className="flex flex-wrap gap-3">
          <button type="submit" disabled={!arquivo || enviando} className="btn-primario">
            {enviando ? 'Importando…' : 'Importar planilha'}
          </button>
          <button type="button" disabled={recalculando} onClick={aoRecalcular} className="btn-secundario">
            {recalculando ? 'Recalculando…' : 'Recalcular riscos'}
          </button>
        </div>
      </form>

      {erro && <Erro mensagem={erro} />}

      {recalculo && (
        <div className="rounded-xl border border-risco-baixo/30 bg-emerald-50 px-5 py-4 text-sm text-risco-baixo">
          Risco recalculado para <strong>{recalculo.recalculados}</strong> aluno(s).
        </div>
      )}

      {resultado && (
        <div className="rounded-xl border border-risco-baixo/30 bg-emerald-50 px-5 py-4 text-sm text-risco-baixo">
          <p>
            <strong>{resultado.registros}</strong> registro(s) · <strong>{resultado.alunos}</strong> aluno(s).
          </p>
          {resultado.erros?.length > 0 && (
            <ul className="mt-2 list-inside list-disc text-risco-alto">
              {resultado.erros.map((msg) => (
                <li key={msg}>{msg}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <div className="painel p-6">
        <h2 className="text-lg font-extrabold">Formato esperado</h2>
        <pre className="mt-4 overflow-x-auto rounded-lg bg-tinta p-4 text-xs text-slate-100">{EXEMPLO_CSV}</pre>
        <ul className="mt-4 list-inside list-disc space-y-1 text-sm text-slate-600">
          <li>Separador: vírgula ou ponto e vírgula</li>
          <li>data no formato AAAA-MM-DD</li>
          <li>frequencia 0–100 · nota 0–10</li>
        </ul>
      </div>
    </section>
  )
}