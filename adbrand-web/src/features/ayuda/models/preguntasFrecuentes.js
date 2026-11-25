// Preguntas frecuentes de la página de Ayuda, agrupadas por tema.
import { CORREO_CONTACTO } from '../../../shared/config/contacto'

export const TEMAS_AYUDA = [
  {
    tema: 'Primeros pasos',
    preguntas: [
      {
        pregunta: '¿Por dónde empiezo?',
        respuesta:
          'Completa "Mi negocio" (rubro, productos, público y tono). Luego genera tu kit de marca y después crea tu primer anuncio. Si quieres un recorrido guiado, ve a Mi cuenta → "Ver el tutorial otra vez".',
      },
      {
        pregunta: '¿Necesito saber de diseño?',
        respuesta: 'No. La IA arma los textos, las imágenes, la paleta de colores y la tipografía. Tú solo eliges y ajustas lo que quieras.',
      },
    ],
  },
  {
    tema: 'Anuncios',
    preguntas: [
      {
        pregunta: '¿Cómo creo un anuncio?',
        respuesta:
          'En "Crear anuncio" elige la red social y escribe tu oferta. La IA te propone varias opciones con texto e imagen; escoges una y, si quieres, la retocas.',
      },
      {
        pregunta: '¿Por qué mi anuncio dice "En revisión" o fue rechazado?',
        respuesta:
          'Cada anuncio pasa por un filtro y, si hace falta, por un moderador, para evitar publicidad engañosa o prohibida. En "Mis anuncios" ves el estado y, si se rechazó, el motivo para corregirlo.',
      },
      {
        pregunta: '¿Puedo programar cuándo se publica?',
        respuesta: 'Sí. En "Calendario" programas tus anuncios aprobados por fecha, hora y red social, y puedes agruparlos en campañas.',
      },
    ],
  },
  {
    tema: 'Planes y pagos',
    preguntas: [
      {
        pregunta: '¿Qué son los tokens de IA?',
        respuesta:
          'Es la medida de cuánto usas la IA al crear anuncios y kits. Cada plan trae una cantidad al mes; en Inicio ves cuánto llevas usado. El plan gratuito incluye 10 000 tokens al mes.',
      },
      {
        pregunta: '¿Cómo pago un plan?',
        respuesta:
          'En "Planes" eliges el plan y pagas con tarjeta o con Yape a través de Mercado Pago. AdBrand.AI no guarda los datos de tu tarjeta.',
      },
      {
        pregunta: '¿Qué pasa si se me acaban los tokens?',
        respuesta: 'La IA deja de generar hasta el próximo mes o hasta que cambies a un plan con más capacidad. Tus anuncios y tu kit se quedan guardados.',
      },
    ],
  },
  {
    tema: 'Cuenta y seguridad',
    preguntas: [
      {
        pregunta: 'Olvidé mi contraseña, ¿qué hago?',
        respuesta: 'En "Iniciar sesión" toca "¿Olvidaste tu contraseña?". Te enviamos un código de 6 números a tu correo para crear una nueva.',
      },
      {
        pregunta: '¿Puedo usar la app del celular con la misma cuenta?',
        respuesta: 'Sí. Entra en la app con el mismo correo y contraseña: verás tus mismos anuncios, kit y plan.',
      },
      {
        pregunta: '¿Cómo pido que borren mis datos?',
        respuesta: `Escríbenos a ${CORREO_CONTACTO} desde el correo de tu cuenta. Más detalles en la Política de privacidad.`,
      },
    ],
  },
]

// Busca en preguntas y respuestas, sin importar mayúsculas ni tildes.
export function filtrarPreguntas(temas, texto) {
  const normalizar = (valor) => valor.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
  const buscado = normalizar(texto.trim())
  if (!buscado) return temas
  return temas
    .map((grupo) => ({
      ...grupo,
      preguntas: grupo.preguntas.filter(
        ({ pregunta, respuesta }) => normalizar(pregunta).includes(buscado) || normalizar(respuesta).includes(buscado),
      ),
    }))
    .filter((grupo) => grupo.preguntas.length > 0)
}
