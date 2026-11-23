// Autor: Lucas Canto (também alterado por Cadu Spadari)
// Selo colorido do nível de risco, usado no painel, no detalhe do aluno e nos avisos.
import { NIVEIS } from '../formatos.js'

// Selo de cor sólida, como no protótipo: ALTO vermelho, MÉDIO laranja, BAIXO verde.
const estilos = {
  ALTO: 'bg-[#C0261B] text-white',
  MEDIO: 'bg-[#E46C0A] text-white',
  BAIXO: 'bg-[#4E8B32] text-white',
}

export default function BadgeRisco({ nivel, grande = false }) {
  const classe = estilos[nivel] ?? 'bg-slate-200 text-slate-600'
  const rotulo = grande ? NIVEIS[nivel]?.titulo : NIVEIS[nivel]?.rotulo
  return (
    <span
      className={`chip justify-center uppercase tracking-wide shadow-sm ${classe} ${
        grande ? 'min-w-[8rem] rounded-lg px-4 py-2 text-sm' : 'min-w-[4.75rem] rounded-md'
      }`}
    >
      {rotulo ?? 'Sem dados'}
    </span>
  )
}
