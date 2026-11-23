// Autor: Lucas Canto
// Equipe (só coordenador): quem tem acesso, redefinir senha (senha temporária) e remover acesso.
import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { FUNCOES } from '../auth.js'
import { Carregando, Erro } from '../components/Estado.jsx'
import { IconeAtencao, IconeCheck } from '../components/Icones.jsx'

function formatarDia(iso) {
  if (!iso) return '—'
  const data = new Date(iso)
  return Number.isNaN(data.getTime()) ? '—' : data.toLocaleDateString('pt-BR')
}

function Pessoa({ pessoa, onRemovida }) {
  const [confirmando, setConfirmando] = useState(null) // 'remover' | 'senha'
  const [senhaTemporaria, setSenhaTemporaria] = useState(null)
  const [trabalhando, setTrabalhando] = useState(false)
  const [erro, setErro] = useState(null)

  async function executar() {
    setTrabalhando(true)
    setErro(null)
    try {
      if (confirmando === 'remover') {
        await api.removerUsuario(pessoa.email)
        onRemovida(pessoa.email)
        return
      }
      const r = await api.redefinirSenha(pessoa.email)
      setSenhaTemporaria(r.senhaTemporaria)
      setConfirmando(null)
    } catch (e) {
      setErro(e.message)
    } finally {
      setTrabalhando(false)
    }
  }

  return (
    <li className="px-6 py-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="font-bold text-tinta">
            {pessoa.nome}
            {pessoa.voce && <span className="chip ml-2 bg-acento/10 text-acento">Você</span>}
          </p>
          <p className="text-sm text-slate-500">{pessoa.email}</p>
          <p className="mt-0.5 text-xs text-slate-400">
            {FUNCOES[pessoa.funcao]} · desde {formatarDia(pessoa.criadoEm)}
          </p>
        </div>
        {!pessoa.voce && !confirmando && (
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={() => { setConfirmando('senha'); setSenhaTemporaria(null) }} className="btn-secundario text-xs">
              Redefinir senha
            </button>
            <button type="button" onClick={() => setConfirmando('remover')} className="btn-secundario text-xs text-risco-alto hover:border-risco-alto">
              Remover acesso
            </button>
          </div>
        )}
        {confirmando && (
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="font-semibold text-slate-600">
              {confirmando === 'remover'
                ? `Remover o acesso de ${pessoa.nome.split(' ')[0]}?`
                : `Criar uma senha temporária para ${pessoa.nome.split(' ')[0]}?`}
            </span>
            <button
              type="button"
              onClick={executar}
              disabled={trabalhando}
              className={`btn-primario text-xs ${confirmando === 'remover' ? 'bg-risco-alto' : ''}`}
            >
              {trabalhando ? 'Aguarde…' : confirmando === 'remover' ? 'Sim, remover' : 'Sim, redefinir'}
            </button>
            <button type="button" onClick={() => setConfirmando(null)} className="btn-secundario text-xs">Cancelar</button>
          </div>
        )}
      </div>
      {senhaTemporaria && (
        <div className="mt-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-slate-700">
          <p className="flex items-center gap-2 font-bold text-risco-baixo">
            <IconeCheck className="h-4 w-4" />
            Senha temporária criada
          </p>
          <p className="mt-2">
            Passe esta senha para {pessoa.nome.split(' ')[0]}:{' '}
            <code className="rounded-md bg-white px-2 py-1 text-base font-bold tracking-wider text-tinta ring-1 ring-linha">
              {senhaTemporaria}
            </code>
          </p>
          <p className="mt-2 text-xs text-slate-500">
            Ela só aparece agora. Ao entrar, a pessoa será lembrada de criar uma senha nova.
          </p>
        </div>
      )}
      {erro && <p className="mt-2 text-sm text-risco-alto">{erro}</p>}
    </li>
  )
}

export default function Equipe() {
  const [equipe, setEquipe] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    api.listarEquipe().then(setEquipe).catch((e) => setErro(e.message))
  }, [])

  if (erro) return <Erro mensagem={erro} />
  if (!equipe) return <Carregando texto="Carregando equipe…" />

  const coordenadores = equipe.filter((p) => p.funcao === 'coordenador').length
  const professores = equipe.filter((p) => p.funcao === 'professor').length

  return (
    <section className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="titulo">Equipe</h1>
        <p className="subtitulo">Quem tem acesso ao sistema. Aqui você redefine senhas e remove quem saiu da escola.</p>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="kpi">
          <p className="kpi-rotulo">Pessoas com acesso</p>
          <p className="kpi-valor">{equipe.length}</p>
        </div>
        <div className="kpi">
          <p className="kpi-rotulo">Coordenadores</p>
          <p className="kpi-valor">{coordenadores}</p>
        </div>
        <div className="kpi">
          <p className="kpi-rotulo">Professores</p>
          <p className="kpi-valor">{professores}</p>
        </div>
      </div>

      <div className="painel overflow-hidden">
        <ul className="divide-y divide-linha">
          {equipe.map((pessoa) => (
            <Pessoa
              key={pessoa.email}
              pessoa={pessoa}
              onRemovida={(email) => setEquipe((lista) => lista.filter((p) => p.email !== email))}
            />
          ))}
        </ul>
      </div>

      <p className="flex items-start gap-2 text-xs text-slate-500">
        <IconeAtencao className="mt-0.5 h-4 w-4 shrink-0" />
        Novas pessoas entram pela tela &quot;Criar conta&quot;, usando o código da escola. Ao remover o acesso, a pessoa é
        desconectada na hora; as providências que ela registrou continuam no histórico dos alunos.
      </p>
    </section>
  )
}
