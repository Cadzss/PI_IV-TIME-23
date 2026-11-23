// Autor: Lucas Canto (também alterado por Cadu Spadari)
// Mensagens padrão de "carregando" e de erro, para todas as telas ficarem iguais.
import { IconeAtencao } from './Icones.jsx'

export function Carregando({ texto = 'Carregando…' }) {
  return (
    <div className="painel flex items-center justify-center gap-3 px-5 py-12 text-sm text-slate-500">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-slate-200 border-t-acento" />
      {texto}
    </div>
  )
}

export function Erro({ mensagem }) {
  return (
    <div className="flex items-start gap-3 rounded-2xl border border-risco-alto/20 bg-risco-alerta px-5 py-4 text-sm text-risco-alto">
      <IconeAtencao className="mt-0.5 h-5 w-5 shrink-0" />
      <p>{mensagem}</p>
    </div>
  )
}
