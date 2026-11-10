# adbrand-admin-web

Web de administración de AdBrand.AI, hecha con React 19 y Vite. Solo habla con Django (`adbrand-admin-api`).

## Estructura (MVVM)

```
src/
├── app/
│   ├── context/      Sesión del administrador
│   └── routes/       Pantallas que puede ver cada rol
├── shared/
│   ├── components/   Menú lateral del administrador (MenuAdmin)
│   └── services/     apiClient.js: el único cliente HTTP hacia Django, con el token de sesión
└── features/         Un módulo por tema, cada uno con models/, viewmodels/ y views/
    ├── auth/           Inicio de sesión del administrador
    ├── admin/          HU 1, 2 y 3: crear administradores, cambiar su rol y desactivarlos
    ├── auditoria/      HU 4: registro de acciones
    ├── suscripciones/  HU 5 a 8: panel financiero y deudas
    ├── moderacion/     HU 14 a 16: bandeja, aprobar o rechazar, historial
    ├── tokens/         HU 17, 19 y 20: consumo de IA, alertas e historial
    └── servidor/       HU 18: estado del servidor
```

El menú muestra solo las pantallas del rol (SUPERADMIN, MODERADOR, FINANZAS o TECNICO), pero quien decide los permisos es Django.

## Comandos

```bash
npm install
npm run dev -- --port 5174   # http://localhost:5174
npm test                     # pruebas con Vitest
npm run lint                 # ESLint
npm run build                # versión para entregar
```
