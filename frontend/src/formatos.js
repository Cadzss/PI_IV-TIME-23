// Autor: Lucas Canto
// Textos e formatos exibidos na tela, em linguagem simples.
// Centralizar aqui evita cada tela formatar data/nota de um jeito diferente.

export const NIVEIS = {
  ALTO: {
    rotulo: 'Alto',
    titulo: 'Risco alto',
    descricao: 'Precisa de ação urgente',
  },
  MEDIO: {
    rotulo: 'Médio',
    titulo: 'Risco médio',
    descricao: 'Acompanhar de perto',
  },
  BAIXO: {
    rotulo: 'Baixo',
    titulo: 'Risco baixo',
    descricao: 'Situação tranquila',
  },
}

export function rotuloNivel(nivel) {
  return NIVEIS[nivel]?.rotulo ?? '—'
}

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

/** "2026-11-01" → "01/11/2026" */
export function formatarData(iso) {
  if (!iso) return '—'
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}

/** "2026-11-01" → "nov/26" */
export function formatarMesCurto(iso) {
  const [ano, mes] = iso.split('-')
  return `${MESES[Number(mes) - 1]}/${ano.slice(2)}`
}

/** Data/hora vinda do banco → "23/11/2026 às 14:05" */
export function formatarDataHora(valor) {
  if (!valor) return '—'
  const data = new Date(valor.$date ?? valor)
  if (Number.isNaN(data.getTime())) return '—'
  const dia = data.toLocaleDateString('pt-BR')
  const hora = data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
  return `${dia} às ${hora}`
}

export function formatarPercentual(valor) {
  return valor == null || Number.isNaN(Number(valor)) ? '—' : `${Number(valor).toFixed(0)}%`
}

export function formatarNota(valor) {
  return valor == null || Number.isNaN(Number(valor))
    ? '—'
    : Number(valor).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}
