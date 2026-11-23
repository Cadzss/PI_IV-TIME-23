// Autor: Lucas Canto
// Ícones simples em SVG (traço), herdam a cor do texto.
// Desenhados à mão em vez de instalar uma biblioteca de ícones: são poucos e assim o site fica mais leve.
function Icone({ children, className = 'h-5 w-5' }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

export function IconeLogo({ className }) {
  return (
    <Icone className={className}>
      <path d="M12 3 2 8l10 5 10-5-10-5Z" />
      <path d="M6 10.5V16c0 1.5 2.7 3 6 3s6-1.5 6-3v-5.5" />
    </Icone>
  )
}

export function IconeSino({ className }) {
  return (
    <Icone className={className}>
      <path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </Icone>
  )
}

export function IconeEnviar({ className }) {
  return (
    <Icone className={className}>
      <path d="M12 16V4" />
      <path d="m7 9 5-5 5 5" />
      <path d="M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3" />
    </Icone>
  )
}

export function IconePainel({ className }) {
  return (
    <Icone className={className}>
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </Icone>
  )
}

export function IconePessoa({ className }) {
  return (
    <Icone className={className}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </Icone>
  )
}

export function IconeSair({ className }) {
  return (
    <Icone className={className}>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="m16 17 5-5-5-5" />
      <path d="M21 12H9" />
    </Icone>
  )
}

export function IconeBusca({ className }) {
  return (
    <Icone className={className}>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </Icone>
  )
}

export function IconeCheck({ className }) {
  return (
    <Icone className={className}>
      <circle cx="12" cy="12" r="9" />
      <path d="m8 12 3 3 5-6" />
    </Icone>
  )
}

export function IconeAtencao({ className }) {
  return (
    <Icone className={className}>
      <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
      <path d="M12 9v4" />
      <path d="M12 17h.01" />
    </Icone>
  )
}

export function IconeVoltar({ className }) {
  return (
    <Icone className={className}>
      <path d="m15 18-6-6 6-6" />
    </Icone>
  )
}

export function IconeArquivo({ className }) {
  return (
    <Icone className={className}>
      <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9l-6-6Z" />
      <path d="M14 3v6h6" />
      <path d="M8 13h8M8 17h5" />
    </Icone>
  )
}

export function IconeOlho({ className }) {
  return (
    <Icone className={className}>
      <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" />
      <circle cx="12" cy="12" r="3" />
    </Icone>
  )
}

export function IconeOlhoFechado({ className }) {
  return (
    <Icone className={className}>
      <path d="M9.9 4.24A10.7 10.7 0 0 1 12 4c6.4 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19" />
      <path d="M6.6 6.6C3.9 8.4 2 12 2 12s3.6 7 10 7a9.7 9.7 0 0 0 5.4-1.6" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
      <path d="m2 2 20 20" />
    </Icone>
  )
}

export function IconeAtualizar({ className }) {
  return (
    <Icone className={className}>
      <path d="M21 12a9 9 0 1 1-3-6.7L21 8" />
      <path d="M21 3v5h-5" />
    </Icone>
  )
}
