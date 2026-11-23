import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { cadastrar, estaLogado, FUNCOES, FUNCOES_COM_SENHA_INSTITUCIONAL } from '../auth.js'

const DESCRICOES = {
  coordenador: 'Vê painel completo, alertas e importação.',
  professor: 'Vê painel e importa planilha de notas/frequência.',
  aluno: 'Consulta o painel de risco (sem importar nem alertas).',
}

const VAZIO = { nome: '', email: '', senha: '', confirmarSenha: '', senhaInstitucional: '' }

export default function Cadastro({ onEntrou }) {
  const navigate = useNavigate()
  const [funcao, setFuncao] = useState('')
  const [dados, setDados] = useState(VAZIO)
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const precisaSenhaInstitucional = FUNCOES_COM_SENHA_INSTITUCIONAL.includes(funcao)

  if (estaLogado()) return <Navigate to="/" replace />

  function atualizar(nome) {
    return (e) => setDados((d) => ({ ...d, [nome]: e.target.value }))
  }

  async function enviar(e) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await cadastrar({ ...dados, funcao })
      onEntrou?.()
      navigate('/', { replace: true })
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="w-full max-w-xl">
        <h1 className="titulo text-center">Criar conta</h1>
        <p className="subtitulo text-center">
          Aluno se cadastra livre. Coordenador e professor precisam da senha institucional.
        </p>

        <form onSubmit={enviar} className="painel mt-6 space-y-4 p-6">
          <fieldset>
            <legend className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500">Função</legend>
            <div className="grid grid-cols-3 gap-2">
              {Object.entries(FUNCOES).map(([chave, nome]) => (
                <label
                  key={chave}
                  className={`cursor-pointer rounded-lg border px-3 py-3 text-center text-sm font-bold ${
                    funcao === chave ? 'border-acento bg-acento text-white' : 'border-linha bg-white text-tinta'
                  }`}
                >
                  <input
                    type="radio"
                    name="funcao"
                    value={chave}
                    checked={funcao === chave}
                    onChange={() => setFuncao(chave)}
                    className="sr-only"
                  />
                  {nome}
                </label>
              ))}
            </div>
            <p className="mt-2 text-xs text-slate-500">{funcao ? DESCRICOES[funcao] : 'Escolha uma função.'}</p>
          </fieldset>

          {funcao && (
            <>
              <label className="rotulo">
                Nome completo
                <input className="campo" required value={dados.nome} onChange={atualizar('nome')} />
              </label>
              <label className="rotulo">
                E-mail
                <input className="campo" type="email" required value={dados.email} onChange={atualizar('email')} />
              </label>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="rotulo">
                  Senha
                  <input className="campo" type="password" required minLength={6} value={dados.senha} onChange={atualizar('senha')} />
                </label>
                <label className="rotulo">
                  Confirmar senha
                  <input className="campo" type="password" required value={dados.confirmarSenha} onChange={atualizar('confirmarSenha')} />
                </label>
              </div>
              {precisaSenhaInstitucional && (
                <label className="rotulo">
                  Senha institucional
                  <input
                    className="campo"
                    type="password"
                    required
                    value={dados.senhaInstitucional}
                    onChange={atualizar('senhaInstitucional')}
                  />
                  <span className="font-medium normal-case tracking-normal text-slate-500">Demo: escola-2026</span>
                </label>
              )}
            </>
          )}

          {erro && <p className="rounded-lg bg-risco-alerta px-3 py-2 text-sm text-risco-alto">{erro}</p>}

          <button type="submit" disabled={enviando || !funcao} className="btn-primario w-full">
            {enviando ? 'Criando…' : 'Criar conta'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-500">
          Já tem conta?{' '}
          <Link to="/login" className="font-bold text-acento">
            Entrar
          </Link>
        </p>
      </div>
    </div>
  )
}
