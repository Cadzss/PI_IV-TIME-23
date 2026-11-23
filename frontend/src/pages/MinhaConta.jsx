// Autor: Lucas Canto
// Minha conta: trocar o nome e alterar a senha (o servidor confere a senha atual).
import { useState } from 'react'
import { api } from '../api.js'
import { atualizarSessao, FUNCOES, sessaoAtual } from '../auth.js'
import { IconeCheck } from '../components/Icones.jsx'
import CampoSenha from '../components/CampoSenha.jsx'

function Mensagem({ tipo, texto }) {
  if (!texto) return null
  return (
    <p
      className={`flex items-center gap-2 rounded-xl px-3 py-2 text-sm ${
        tipo === 'ok' ? 'bg-emerald-50 text-risco-baixo' : 'bg-risco-alerta text-risco-alto'
      }`}
    >
      {tipo === 'ok' && <IconeCheck className="h-4 w-4 shrink-0" />}
      {texto}
    </p>
  )
}

function DadosPessoais({ sessao }) {
  const [nome, setNome] = useState(sessao.nome)
  const [salvando, setSalvando] = useState(false)
  const [mensagem, setMensagem] = useState(null)

  async function salvar(e) {
    e.preventDefault()
    setSalvando(true)
    setMensagem(null)
    try {
      const atualizado = await api.atualizarPerfil(nome)
      atualizarSessao({ nome: atualizado.nome })
      setNome(atualizado.nome)
      setMensagem({ tipo: 'ok', texto: 'Nome atualizado.' })
    } catch (err) {
      setMensagem({ tipo: 'erro', texto: err.message })
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form onSubmit={salvar} className="painel space-y-4 p-6">
      <div>
        <h2 className="text-lg font-extrabold">Seus dados</h2>
        <p className="mt-1 text-sm text-slate-500">O nome aparece no topo da tela e nas providências que você registra.</p>
      </div>
      <label className="rotulo">
        Nome completo
        <input className="campo" required minLength={3} maxLength={100} value={nome} onChange={(e) => setNome(e.target.value)} />
      </label>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rotulo">
          E-mail
          <p className="campo bg-fundo/60 text-slate-500">{sessao.email}</p>
        </div>
        <div className="rotulo">
          Função
          <p className="campo bg-fundo/60 text-slate-500">{FUNCOES[sessao.funcao]}</p>
        </div>
      </div>
      <p className="text-xs text-slate-500">Para trocar o e-mail ou a função, fale com a coordenação.</p>
      <Mensagem {...(mensagem ?? {})} />
      <button type="submit" disabled={salvando || nome.trim() === sessao.nome} className="btn-primario">
        {salvando ? 'Salvando…' : 'Salvar nome'}
      </button>
    </form>
  )
}

function AlterarSenha({ temporaria }) {
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [mensagem, setMensagem] = useState(null)

  async function salvar(e) {
    e.preventDefault()
    setMensagem(null)
    if (nova !== confirmacao) {
      setMensagem({ tipo: 'erro', texto: 'As duas senhas novas não são iguais.' })
      return
    }
    setSalvando(true)
    try {
      await api.alterarSenha(atual, nova)
      atualizarSessao({ senhaTemporaria: false })
      setAtual('')
      setNova('')
      setConfirmacao('')
      setMensagem({ tipo: 'ok', texto: 'Senha alterada. Se você estava conectado em outro computador, vai precisar entrar de novo lá.' })
    } catch (err) {
      setMensagem({ tipo: 'erro', texto: err.message })
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form onSubmit={salvar} className={`painel space-y-4 p-6 ${temporaria ? 'border-l-4 border-l-[#E46C0A]' : ''}`}>
      <div>
        <h2 className="text-lg font-extrabold">Alterar senha</h2>
        <p className="mt-1 text-sm text-slate-500">
          {temporaria
            ? 'Você está usando uma senha temporária. Crie uma senha só sua.'
            : 'Use pelo menos 6 caracteres.'}
        </p>
      </div>
      <label className="rotulo">
        Senha atual{temporaria ? ' (a temporária que você recebeu)' : ''}
        <CampoSenha autoComplete="current-password" required value={atual} onChange={(e) => setAtual(e.target.value)} />
      </label>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="rotulo">
          Nova senha
          <CampoSenha autoComplete="new-password" required minLength={6} value={nova} onChange={(e) => setNova(e.target.value)} />
        </label>
        <label className="rotulo">
          Repita a nova senha
          <CampoSenha autoComplete="new-password" required value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)} />
        </label>
      </div>
      <Mensagem {...(mensagem ?? {})} />
      <button type="submit" disabled={salvando} className="btn-primario">
        {salvando ? 'Alterando…' : 'Alterar senha'}
      </button>
    </form>
  )
}

export default function MinhaConta() {
  const sessao = sessaoAtual()

  return (
    <section className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="titulo">Minha conta</h1>
        <p className="subtitulo">Seus dados de acesso ao sistema.</p>
      </div>
      <DadosPessoais sessao={sessao} />
      <AlterarSenha temporaria={sessao.senhaTemporaria} />
    </section>
  )
}
