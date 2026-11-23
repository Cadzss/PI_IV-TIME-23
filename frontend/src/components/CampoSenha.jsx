// Autor: Lucas Canto
import { useState } from 'react'
import { IconeOlho, IconeOlhoFechado } from './Icones.jsx'

/** Campo de senha com o botão de olho para mostrar/ocultar o que foi digitado. */
export default function CampoSenha(props) {
  const [visivel, setVisivel] = useState(false)
  return (
    <span className="relative block">
      <input
        {...props}
        type={visivel ? 'text' : 'password'}
        className="campo"
        // .campo define o padding; o espaço do botão vai inline para não ficar embaixo do texto
        style={{ paddingRight: '2.75rem' }}
      />
      <button
        type="button"
        onClick={() => setVisivel((v) => !v)}
        aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}
        aria-pressed={visivel}
        title={visivel ? 'Ocultar senha' : 'Mostrar senha'}
        className="absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-r-lg text-slate-400 transition hover:text-tinta focus:text-tinta focus:outline-none"
      >
        {visivel ? <IconeOlhoFechado className="h-5 w-5" /> : <IconeOlho className="h-5 w-5" />}
      </button>
    </span>
  )
}
