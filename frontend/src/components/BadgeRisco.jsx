const estilos = {
  ALTO: { classe: 'bg-risco-alerta text-risco-alto', rotulo: 'ALTO' },
  MEDIO: { classe: 'bg-amber-50 text-risco-medio', rotulo: 'MÉDIO' },
  BAIXO: { classe: 'bg-emerald-50 text-risco-baixo', rotulo: 'BAIXO' },
}

export function rotuloRisco(nivel) {
  return estilos[nivel]?.rotulo ?? nivel
}

export default function BadgeRisco({ nivel, grande = false }) {
  const e = estilos[nivel] ?? { classe: 'bg-slate-100 text-slate-600', rotulo: nivel ?? '—' }
  return (
    <span className={`chip ${e.classe} ${grande ? 'px-3 py-1.5 text-sm' : ''}`}>
      {e.rotulo}
    </span>
  )
}
