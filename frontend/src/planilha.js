// Autor: Lucas Canto
// Leitura da planilha escolhida pelo usuário (.xlsx do Excel ou .csv) e geração da planilha modelo.
// O .xlsx é convertido aqui no navegador para o mesmo texto CSV que o servidor já sabe validar.

const COLUNAS_PERCENTUAIS = ['frequencia', 'atividades']

/** "Frequência " → "frequencia" (mesma regra do servidor). */
function normalizarCabecalho(nome) {
  return String(nome ?? '')
    .trim()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
}

function doisDigitos(n) {
  return String(n).padStart(2, '0')
}

/** Data do Excel → "AAAA-MM-DD". Datas do .xlsx não têm fuso: usamos UTC para não "voltar um dia". */
function dataIso(data) {
  return `${data.getUTCFullYear()}-${doisDigitos(data.getUTCMonth() + 1)}-${doisDigitos(data.getUTCDate())}`
}

/** Data guardada como número de série do Excel (quando a célula não está formatada como data). */
function dataDeNumeroExcel(n) {
  return dataIso(new Date(Date.UTC(1899, 11, 30) + Math.round(n) * 86400000))
}

function celulaParaTexto(valor) {
  if (valor == null) return ''
  if (valor instanceof Date) return dataIso(valor)
  return String(valor).trim()
}

function campoCsv(texto) {
  return /[",\n\r]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto
}

/** Linhas do Excel (arrays de células) → texto CSV. */
export function linhasParaCsv(linhas) {
  const preenchidas = linhas.filter((linha) => linha.some((c) => c != null && String(c).trim() !== ''))
  if (preenchidas.length === 0) return ''

  const cabecalho = preenchidas[0].map(normalizarCabecalho)
  const corpo = preenchidas.slice(1).map((linha) => [...linha])

  // Excel com célula em formato "%" guarda 62% como 0,62: se a coluna inteira estiver entre 0 e 1, converte.
  for (const nome of COLUNAS_PERCENTUAIS) {
    const i = cabecalho.indexOf(nome)
    if (i < 0) continue
    const numeros = corpo.map((l) => l[i]).filter((v) => typeof v === 'number')
    if (numeros.length > 0 && numeros.every((v) => v >= 0 && v <= 1) && numeros.some((v) => v > 0)) {
      for (const l of corpo) {
        if (typeof l[i] === 'number') l[i] = Math.round(l[i] * 10000) / 100
      }
    }
  }

  const iData = cabecalho.indexOf('data')
  if (iData >= 0) {
    for (const l of corpo) {
      if (typeof l[iData] === 'number') l[iData] = dataDeNumeroExcel(l[iData])
    }
  }

  return [cabecalho, ...corpo]
    .map((linha) => linha.map((c) => campoCsv(celulaParaTexto(c))).join(','))
    .join('\n')
}

/** Lê o arquivo escolhido e devolve o conteúdo em CSV. */
export async function lerPlanilha(arquivo) {
  const nome = arquivo.name.toLowerCase()
  if (nome.endsWith('.xlsx')) {
    const { readSheet } = await import('read-excel-file/browser')
    let linhas
    try {
      linhas = await readSheet(arquivo)
    } catch {
      throw new Error('Não conseguimos abrir esse arquivo do Excel. Confira se ele não está corrompido ou protegido por senha.')
    }
    return linhasParaCsv(linhas)
  }
  if (nome.endsWith('.xls')) {
    throw new Error('Esse é o formato antigo do Excel (.xls). Abra no Excel e use Arquivo › Salvar como › Pasta de Trabalho do Excel (.xlsx).')
  }
  if (nome.endsWith('.csv') || nome.endsWith('.txt')) {
    return arquivo.text()
  }
  throw new Error('Escolha uma planilha do Excel (.xlsx) ou um arquivo .csv.')
}

/** Baixa a planilha modelo, já com as colunas certas e um exemplo. */
export async function baixarModelo() {
  const { default: writeExcelFile } = await import('write-excel-file/browser')
  const titulo = (value) => ({ value, fontWeight: 'bold', backgroundColor: '#E8EEF4' })
  const data = (dia, mes, ano) => ({ value: new Date(Date.UTC(ano, mes - 1, dia)), type: Date, format: 'dd/mm/yyyy' })

  const linhas = [
    ['matricula', 'nome', 'turma', 'data', 'frequencia', 'nota', 'atrasos', 'atividades'].map(titulo),
    ['2026001', 'João Pereira', '8ºA', data(1, 11, 2026), 62, 5.2, 6, 47],
    ['2026002', 'Maria Souza', '9ºB', data(1, 11, 2026), 78, 6.8, 4, 73],
  ]
  const colunas = [{ width: 12 }, { width: 24 }, { width: 8 }, { width: 12 }, { width: 12 }, { width: 8 }, { width: 10 }, { width: 12 }]
  await writeExcelFile(linhas, { columns: colunas }).toFile('planilha-modelo-early-warning.xlsx')
}
