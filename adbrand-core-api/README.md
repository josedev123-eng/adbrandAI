# adbrand-core-api

Backend del usuario PYME de AdBrand.AI, hecho con Spring Boot 4.1.1 y Java 17. Lo usan la web del usuario (`adbrand-web`) y la app móvil (`adbrand-mobile`). Es el único que llama al servidor de IA y el dueño de las migraciones de la base de datos.

## Estructura

```
src/main/java/com/adbrand/core/
├── config/            CORS y configuración general
├── shared/error/      Formato único de error {codigo, mensaje, campos}
├── auth/              HU 21, 22, 24 y 43: registro, inicio de sesión con JWT, recuperar contraseña, renovar sesión
├── cuenta/            HU 23: datos de la empresa
├── negocio/           HU 9: perfil del negocio
├── ia/                Cliente del servidor de IA de Tecsup y conteo de tokens
├── contenido/         HU 10 y 12: generar y regenerar anuncios
├── revision/          HU 13: filtro del contenido generado
├── brandkit/          HU 11 y 29 a 32: kit de marca y su edición
├── pago/              HU 25 a 28: planes, tarjeta, Yape y comprobante
├── calendario/        HU 33 a 36: calendario, campañas, programar y reprogramar
├── reporte/           HU 37 a 40: informe de campañas, filtros, PDF y Word
├── previsualizacion/  HU 45: kit y anuncios para la app móvil
├── estado/            HU 18: estado del backend y de la IA
└── notificacion/      Envío de correos
src/main/resources/
├── application.properties
└── db/migration/      Migraciones Flyway de toda la base de datos (V1 a V37)
src/test/java/...      Pruebas, una carpeta por módulo
```

Cada módulo sigue `controller/` → `service/` → `repository/`, más `entity/` y `dto/`. El usuario siempre sale del token de sesión (`@RequestAttribute("usuarioId")`), así cada empresa solo ve sus datos.

## Configuración

Copiar `.env.example` a `.env` y completar los valores. El `.env` no se sube al repositorio.

- Con `AI_MOCK=true`, la IA devuelve textos de prueba, para trabajar fuera de la red de Tecsup.
- Con `MAIL_MOCK=true`, los correos se escriben en la consola en lugar de enviarse.

## Comandos

```bash
.\mvnw spring-boot:run     # http://localhost:8080/api
.\mvnw test                # pruebas (necesitan la base local)
```

Al arrancar, Flyway aplica las migraciones pendientes. Los endpoints están en [`adbrand-docs/api/spring-boot.md`](../adbrand-docs/api/spring-boot.md).
