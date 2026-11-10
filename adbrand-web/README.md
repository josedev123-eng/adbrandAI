# adbrand-web

Web del usuario PYME de AdBrand.AI, hecha con React 19 y Vite. Solo habla con Spring Boot (`adbrand-core-api`).

## Estructura (MVVM)

```
src/
├── app/context/      Sesión del usuario (SesionProvider, useSesion)
├── shared/
│   ├── components/   Menú del usuario (MenuUsuario)
│   └── services/     apiClient.js: el único cliente HTTP hacia Spring Boot, con el token de sesión
└── features/         Un módulo por tema, cada uno con models/, viewmodels/ y views/
    ├── auth/         HU 21, 22 y 24: registro, inicio de sesión, recuperar contraseña
    ├── negocio/      HU 9: perfil del negocio
    ├── cuenta/       HU 23: datos de la empresa
    ├── contenido/    HU 10 y 12: generar y regenerar anuncios
    ├── brandkit/     HU 11 y 29 a 32: kit de marca y su edición
    ├── pagos/        HU 25 a 28: planes, tarjeta, Yape y comprobante
    ├── calendario/   HU 33 a 36: calendario, campañas, programar y reprogramar
    └── reportes/     HU 37 a 40: informe, filtros, PDF y Word
```

- `views/` solo dibuja la pantalla.
- `viewmodels/` tiene el estado y las validaciones.
- `models/` hace las llamadas a la API.

Ninguna vista llama a la API directamente.

## Comandos

```bash
npm install
npm run dev       # http://localhost:5173
npm test          # pruebas con Vitest
npm run lint      # ESLint
npm run build     # versión para entregar
```

La URL de Spring Boot es `http://localhost:8080/api`. Se cambia con la variable `VITE_API_URL`.
