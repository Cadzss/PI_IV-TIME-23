import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import BadgeRisco from '../components/BadgeRisco.jsx'
import { Carregando, Erro } from '../components/Estado.jsx'

// Tela 1 — Dashboard: alunos ordenados por nível de risco, com filtros por turma e nível
export default function Dashboard() {
  const navigate = useNavigate()
  const [turmas, setTurmas] = useState([])
  const [turma, setTurma] = useState('')
  const [nivel, setNivel] = useState('')
  const [alunos, setAlunos] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    api.listarTurmas().then(setTurmas).catch((e) => setErro(e.message))
  }, [])

  useEffect(() => {
    setAlunos(null)
    api
      .listarAlunos({ turma, nivel })
      .then((lista) => {
        setAlunos(lista)
        setErro(null)
      })
      .catch((e) => setErro(e.message))
  }, [turma, nivel])

  const contagem = (n) => alunos?.filter((a) => a.risco?.nivel === n).length ?? 0

  return (
    <section>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-marinho">Alunos por risco de evasão</h1>
          {alunos && (
            <p className="mt-1 text-sm text-slate-500">
              {alunos.length} aluno(s) · {contagem('ALTO')} em risco alto · {contagem('MEDIO')} em risco médio
            </p>
          )}
        </div>
        <div className="flex gap-3">
          <select
            className="rounded border border-slate-300 bg-white px-3 py-2 text-sm"
            value={turma}
            onChange={(e) => setTurma(e.target.value)}
          >
            <option value="">Todas as turmas</option>
            {turmas.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
          <select
            className="rounded border border-slate-300 bg-white px-3 py-2 text-sm"
            value={nivel}
            onChange={(e) => setNivel(e.target.value)}
          >
            <option value="">Todos os níveis</option>
            <option value="ALTO">Alto</option>
            <option value="MEDIO">Médio</option>
            <option value="BAIXO">Baixo</option>
          </select>
        </div>
      </div>

      {erro && <Erro mensagem={erro} />}
      {!erro && !alunos && <Carregando />}

      {alunos && (
        <div className="overflow-hidden rounded-lg bg-white shadow-sm">
          <table className="w-full text-left text-sm">
            <thead className="border-b-2 border-slate-300 text-slate-600">
              <tr>
                <th className="px-5 py-3 font-semibold">Aluno</th>
                <th className="px-5 py-3 font-semibold">Turma</th>
                <th className="px-5 py-3 text-right font-semibold">Frequência</th>
                <th className="px-5 py-3 text-right font-semibold">Notas</th>
                <th className="px-5 py-3 font-semibold">Risco</th>
              </tr>
            </thead>
            <tbody>
              {alunos.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-5 py-8 text-center text-slate-500">
                    Nenhum aluno encontrado. Importe uma planilha para começar.
                  </td>
                </tr>
              )}
              {alunos.map((a) => (
                <tr
                  key={a.matricula}
                  onClick={() => navigate(`/alunos/${a.matricula}`)}
                  className="cursor-pointer border-b border-slate-100 hover:bg-slate-50"
                >
                  <td className="px-5 py-3 font-medium">{a.nome}</td>
                  <td className="px-5 py-3">{a.turma}</td>
                  <td className="px-5 py-3 text-right tabular-nums">{a.risco?.frequenciaAtual?.toFixed(0)}%</td>
                  <td className="px-5 py-3 text-right tabular-nums">{a.risco?.notaAtual?.toFixed(1)}</td>
                  <td className="px-5 py-3">
                    <BadgeRisco nivel={a.risco?.nivel} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="px-5 py-3 text-xs italic text-slate-400">
            → Clique em um aluno para ver o histórico detalhado
          </p>
        </div>
      )}
    </section>
  )
}
