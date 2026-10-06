// Agrega comprobaciones como toBeInTheDocument() y limpia la pantalla después de cada prueba.
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => cleanup())