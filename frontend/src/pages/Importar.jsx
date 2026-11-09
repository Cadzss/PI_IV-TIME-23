import { useState } from 'react'
import { api } from '../api.js'
import { Erro } from '../components/Estado.jsx'

const EXEMPLO = `matricula,nome,turma,data,frequencia,nota
2026001,João P.,8ºA,2026-11-01,62,5.2`

// Importação de frequência e notas via planilha (CSV exportado do Excel / Google Planilhas)
export default function Importar() {
  const [arquivo, setArquivo] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [recalculando, setRecalculando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [recalculo, setRecalculo] = useState(null)
  const [erro, setErro] = useState(null)

  async function importar(e) {
    e.preventDefault()
    if (!arquivo) return
    setEnviando(true)
    setErro(null)
    setResultado(null)
    try {
      const conteudo = await arquivo.text()
      setResultado(await api.importarCsv(conteudo))
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
    }
  }

  async function recalcular() {
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
    <section className="max-w-2xl">
      <h1 className="text-2xl font-bold text-marinho">Importar planilha</h1>
      <p className="mb-6 mt-1 text-sm text-slate-500">
        Envie um arquivo CSV com a frequência e as notas dos alunos. O risco é recalculado e os alertas são
        gerados automaticamente.
      </p>

      <form onSubmit={importar} className="space-y-4 rounded-lg bg-white p-5 shadow-sm">
        <input
          type="file"
          accept=".csv,text/csv"
          onChange={(e) => setArquivo(e.target.files[0] ?? null)}
          className="block w-full text-sm file:mr-4 file:rounded file:border-0 file:bg-slate-200 file:px-4 file:py-2"
        />
        <div className="flex flex-wrap gap-3">
          <button
            type="submit"
            disabled={!arquivo || enviando}
            className="rounded bg-marinho px-5 py-2 text-sm text-white disabled:opacity-40"
          >
            {enviando ? 'Importando…' : 'Importar'}
          </button>
          <button
            type="button"
            onClick={recalcular}
            disabled={recalculando}
            className="rounded border border-marinho px-5 py-2 text-sm text-marinho disabled:opacity-40"
          >
            {recalculando ? 'Recalculando…' : 'Recalcular riscos'}
          </button>
        </div>
      </form>

      {erro && (
        <div className="mt-4">
          <Erro mensagem={erro} />
        </div>
      )}

      {recalculo && (
        <div className="mt-4 rounded border border-green-300 bg-green-50 p-4 text-sm text-green-900">
          <p>
            Risco recalculado para <strong>{recalculo.recalculados}</strong> aluno(s).
          </p>
        </div>
      )}

      {resultado && (
        <div className="mt-4 rounded border border-green-300 bg-green-50 p-4 text-sm text-green-900">
          <p>
            <strong>{resultado.registros}</strong> registro(s) importado(s) para <strong>{resultado.alunos}</strong>{' '}
            aluno(s).
          </p>
          {resultado.erros?.length > 0 && (
            <ul className="mt-2 list-inside list-disc text-red-800">
              {resultado.erros.map((e) => (
                <li key={e}>{e}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <div className="mt-8 text-sm">
        <h2 className="mb-2 font-semibold text-marinho">Formato esperado</h2>
        <pre className="overflow-x-auto rounded bg-slate-800 p-4 text-xs text-slate-100">{EXEMPLO}</pre>
        <ul className="mt-2 list-inside list-disc text-slate-600">
          <li>Separador vírgula ou ponto e vírgula</li>
          <li>
            <code>data</code> no formato AAAA-MM-DD (período de referência)
          </li>
          <li>
            <code>frequencia</code> de 0 a 100 · <code>nota</code> de 0 a 10 (aceita vírgula decimal)
          </li>
        </ul>
      </div>
    </section>
  )
}
