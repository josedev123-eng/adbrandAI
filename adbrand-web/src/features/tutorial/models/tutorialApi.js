// Model: pasos del tutorial guiado y el aviso a Spring de que la empresa ya lo vio (V46).
import { apiRequest } from '../../../shared/services/apiClient'

export function marcarTutorialVisto() {
  return apiRequest('/cuenta/tutorial-visto', { method: 'POST' })
}

// "objetivo" es el data-tutorial del elemento que se resalta. Sin objetivo, la tarjeta sale al centro.
// "color" es el fondo del círculo del emoji; "final" activa el confeti.
export const PASOS_TUTORIAL = [
  {
    emoji: '👋',
    color: 'var(--primary-soft)',
    titulo: '¡Hola! Qué bueno tenerte aquí',
    texto: 'Vamos a darle a tu negocio la publicidad que se merece. Te muestro todo en menos de un minuto, ¿vamos?',
  },
  {
    objetivo: 'perfil',
    emoji: '🏪',
    color: '#fff1e6',
    titulo: 'Primero, cuéntanos de tu negocio',
    texto: 'Tu rubro, tus productos y a quién le vendes. Con eso la IA te conoce y escribe como tú. ¡Es el paso más importante!',
  },
  {
    objetivo: 'kit',
    emoji: '🎨',
    color: 'var(--ia-soft)',
    titulo: 'Dale personalidad a tu marca',
    texto: 'Colores, tipografías y logo. Así cada anuncio dice "esto es de mi negocio" desde el primer vistazo.',
  },
  {
    objetivo: 'anuncio',
    emoji: '✨',
    color: 'var(--primary-soft)',
    titulo: 'Aquí ocurre la magia',
    texto: 'Elige la red social y tu oferta, y la IA te propone varios anuncios con texto e imagen. Escoges tu favorito y lo retocas a tu gusto.',
  },
  {
    objetivo: 'misAnuncios',
    emoji: '🛡️',
    color: 'var(--ok-soft)',
    titulo: 'Publica con tranquilidad',
    texto: 'Revisamos cada anuncio para cuidar tu reputación. Aquí ves si se aprobó y, si algo falló, te decimos qué cambiar.',
  },
  {
    objetivo: 'calendario',
    emoji: '📅',
    color: 'var(--warn-soft)',
    titulo: 'Organiza tu semana en un vistazo',
    texto: 'Programa tus publicaciones por fecha y red social, y agrúpalas en campañas. Adiós a olvidarte de publicar.',
  },
  {
    objetivo: 'planes',
    emoji: '🚀',
    color: '#fff1e6',
    titulo: 'Crece a tu ritmo',
    texto: 'Empiezas gratis. Cuando tu negocio pida más, aquí subes de plan y pagas con tarjeta o Yape.',
  },
  {
    objetivo: 'cuenta',
    emoji: '🎉',
    color: 'var(--primary-soft)',
    titulo: '¡Todo listo para despegar!',
    texto: 'Si alguna vez quieres repasar, este recorrido te espera en Mi Cuenta. Ahora sí: ¡a completar los datos de tu negocio!',
    final: true,
  },
]
