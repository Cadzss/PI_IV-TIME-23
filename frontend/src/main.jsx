// Autor: Lucas Canto
// Ponto de entrada do React: pega a <div id="root"> do index.html e desenha o App dentro dela.
// BrowserRouter liga as rotas (/, /alunos/:matricula, /alertas...) à barra de endereço do navegador.
// StrictMode só serve em desenvolvimento, para o React avisar de práticas problemáticas.
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import './index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
