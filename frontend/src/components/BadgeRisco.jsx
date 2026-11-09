const estilos = {
  ALTO: { cor: 'bg-risco-alto', rotulo: 'ALTO' },
  MEDIO: { cor: 'bg-risco-medio', rotulo: 'MÉDIO' },
  BAIXO: { cor: 'bg-risco-baixo', rotulo: 'BAIXO' },
}

export function rotuloRisco(nivel) {
  return estilos[nivel]?.rotulo ?? nivel
}

export default function BadgeRisco({ nivel, grande = false, prefixo = '' }) {
  const e = estilos[nivel] ?? { cor: 'bg-slate-400', rotulo: nivel ?? '—' }
  return (
    <span
      className={`inline-block rounded text-center font-bold text-white ${e.cor} ${
        grande ? 'px-6 py-2 text-base' : 'w-20 px-2 py-1 text-xs'
      }`}
    >
      {prefixo}
      {e.rotulo}
    </span>
  )
}
