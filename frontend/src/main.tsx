import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@fontsource/manrope/latin-400.css'
import '@fontsource/manrope/latin-500.css'
import '@fontsource/manrope/latin-600.css'
import '@fontsource/manrope/latin-700.css'
import '@fontsource/manrope/latin-800.css'
import '@fontsource/noto-sans-bengali/bengali-400.css'
import '@fontsource/noto-sans-bengali/bengali-500.css'
import '@fontsource/noto-sans-bengali/bengali-600.css'
import '@fontsource/noto-sans-bengali/bengali-700.css'
import App from './App'
import { DemoUserProvider } from './app/DemoUserContext'
import { CurrentAffairsStateProvider } from './features/currentAffairs/CurrentAffairsState'
import { LocaleProvider } from './i18n/LocaleContext'
import './styles.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <LocaleProvider>
        <DemoUserProvider>
          <CurrentAffairsStateProvider>
            <App />
          </CurrentAffairsStateProvider>
        </DemoUserProvider>
      </LocaleProvider>
    </BrowserRouter>
  </StrictMode>,
)
