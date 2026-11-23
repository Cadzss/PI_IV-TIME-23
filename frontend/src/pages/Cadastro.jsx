// Autor: Arthur Zambroni (também alterado por Cadu Spadari)
// Alterações: Lucas Canto — sem conta de aluno (relatório 2.1.3), "código da escola" no lugar de
//             "senha institucional", visual novo, alinhamento dos campos e campos de senha com olho
//
// Tela de criar conta: escolhe a função e, se for coordenador/professor, pede o código da escola.
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { cadastrar, estaLogado, FUNCOES, FUNCOES_COM_SENHA_INSTITUCIONAL, paginaInicial } from '../auth.js'
import { IconeLogo } from '../components/Icones.jsx'
import CampoSenha from '../components/CampoSenha.jsx'

const DESCRICOES = {
  coordenador: 'Vê todos os alunos, recebe avisos e envia planilhas.',
  professor: 'Vê todos os alunos e envia planilhas.',
}

const VAZIO = { nome: '', email: '', senha: '', confirmarSenha: '', senhaInstitucional: '' }

export default function Cadastro({ onEntrou }) {
  const navigate = useNavigate()
  const [funcao, setFuncao] = useState('')
  const [dados, setDados] = useState(VAZIO)
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const precisaCodigoEscola = FUNCOES_COM_SENHA_INSTITUCIONAL.includes(funcao)

  if (estaLogado()) return <Navigate to={paginaInicial() ?? '/'} replace />

  // Gera um onChange para cada campo do formulário (evita escrever um useState por campo)
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
      navigate(paginaInicial() ?? '/', { replace: true })
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="w-full max-w-xl">
        <div className="flex flex-col items-center text-center">
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-tinta text-teal-300">
            <IconeLogo className="h-7 w-7" />
          </span>
          <h1 className="titulo mt-4">Criar conta</h1>
          <p className="subtitulo">Para coordenadores e professores da escola.</p>
        </div>

        <form onSubmit={enviar} className="painel mt-6 space-y-5 p-6">
          <fieldset>
            <legend className="mb-2 text-sm font-semibold text-slate-600">Eu sou</legend>
            <div className="grid grid-cols-2 gap-2">
              {Object.entries(FUNCOES).map(([chave, nome]) => (
                <label
                  key={chave}
                  className={`cursor-pointer rounded-xl border px-3 py-3 text-center text-sm font-bold transition ${
                    funcao === chave
                      ? 'border-tinta bg-tinta text-white'
                      : 'border-linha bg-white text-tinta hover:border-acento/60'
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
            <p className="mt-2 text-xs text-slate-500">{funcao ? DESCRICOES[funcao] : 'Escolha uma opção para continuar.'}</p>
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
                  <CampoSenha required minLength={6} value={dados.senha} onChange={atualizar('senha')} />
                  <span className="text-xs font-normal text-slate-500">Pelo menos 6 caracteres.</span>
                </label>
                <label className="rotulo">
                  Repita a senha
                  <CampoSenha required value={dados.confirmarSenha} onChange={atualizar('confirmarSenha')} />
                </label>
              </div>
              {precisaCodigoEscola && (
                <label className="rotulo">
                  Código da escola
                  <CampoSenha
                    required
                    value={dados.senhaInstitucional}
                    onChange={atualizar('senhaInstitucional')}
                  />
                  <span className="text-xs font-normal text-slate-500">Peça à coordenação da sua escola.</span>
                </label>
              )}
            </>
          )}

          {erro && <p className="rounded-xl bg-risco-alerta px-3 py-2 text-sm text-risco-alto">{erro}</p>}

          <button type="submit" disabled={enviando || !funcao} className="btn-primario w-full py-3">
            {enviando ? 'Criando conta…' : 'Criar conta'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-500">
          Já tem conta?{' '}
          <Link to="/login" className="font-bold text-acento hover:underline">
            Entrar
          </Link>
        </p>
      </div>
    </div>
  )
}
