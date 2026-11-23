export function Carregando() {
  return <div className="painel px-5 py-8 text-sm text-slate-500">Carregando dados do servidor…</div>
}

export function Erro({ mensagem }) {
  return (
    <div className="rounded-xl border border-risco-alto/30 bg-risco-alerta px-5 py-4 text-sm text-risco-alto">
      <strong>Erro:</strong> {mensagem}
    </div>
  )
}
