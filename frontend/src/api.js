// Toda comunicação com o backend passa por aqui:
// React -> POST /api/comando -> PonteHttp (Java) -> Socket -> Servidor -> MongoDB
export async function enviar(acao, dados = {}) {
  let resposta
  try {
    resposta = await fetch('/api/comando', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ acao, dados }),
    })
  } catch {
    throw new Error('Não foi possível conectar à ponte HTTP (porta 8080). Ela está rodando?')
  }
  const json = await resposta.json().catch(() => null)
  if (!json) throw new Error(`Resposta inválida do servidor (HTTP ${resposta.status})`)
  if (!json.ok) throw new Error(json.erro || 'Erro desconhecido')
  return json.dados
}

export const api = {
  listarAlunos: (filtros) => enviar('LISTAR_ALUNOS', filtros),
  detalharAluno: (matricula) => enviar('DETALHAR_ALUNO', { matricula }),
  listarTurmas: () => enviar('LISTAR_TURMAS'),
  listarAlertas: () => enviar('LISTAR_ALERTAS'),
  marcarAlertaLido: (id) => enviar('MARCAR_ALERTA_LIDO', { id }),
  importarCsv: (conteudo) => enviar('IMPORTAR_CSV', { conteudo }),
}

// O driver do Mongo serializa datas como {"$date": ...}
export function lerData(valor) {
  if (!valor) return null
  return new Date(valor.$date ?? valor)
}
