export function Carregando() {
  return <p className="text-slate-500">Carregando…</p>
}

export function Erro({ mensagem }) {
  return (
    <div className="rounded border border-red-300 bg-red-50 p-4 text-sm text-red-800">
      <strong>Erro:</strong> {mensagem}
    </div>
  )
}
