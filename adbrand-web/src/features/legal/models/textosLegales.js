// Textos de Términos y condiciones y de Política de privacidad (Ley N.º 29733, Protección de Datos Personales, Perú).
// Nota: son textos del proyecto del curso. Para usarlos con clientes reales debe revisarlos un abogado.
import { CORREO_CONTACTO } from '../../../shared/config/contacto'

export const ACTUALIZADO = '23 de noviembre de 2026'

export const TERMINOS = {
  titulo: 'Términos y condiciones',
  resumen: 'Las reglas para usar AdBrand.AI: qué ofrecemos, qué esperamos de tu empresa y qué pasa si se incumplen.',
  secciones: [
    {
      titulo: '1. Qué es AdBrand.AI',
      parrafos: [
        'AdBrand.AI es una plataforma que ayuda a las pequeñas y medianas empresas a crear anuncios, kits de marca y calendarios de publicación con inteligencia artificial (IA).',
        'Al crear una cuenta aceptas estos términos. Si no estás de acuerdo, no uses la plataforma.',
      ],
    },
    {
      titulo: '2. Tu cuenta',
      parrafos: [
        'La cuenta es de tu empresa. Debes dar datos reales y mantener tu contraseña en secreto. Eres responsable de lo que se haga con tu cuenta.',
        'Si crees que alguien entró a tu cuenta sin permiso, cambia tu contraseña y escríbenos.',
      ],
    },
    {
      titulo: '3. Contenido generado con IA',
      parrafos: [
        'La IA propone textos e imágenes a partir de los datos de tu negocio. Antes de publicar un anuncio, revísalo: tú decides qué se publica y eres responsable de que sea verdadero y respete la ley.',
        'Los anuncios pasan por una revisión de moderación. Podemos rechazar un anuncio con información engañosa, ofensiva, ilegal o que infrinja derechos de otras personas, y te diremos el motivo.',
      ],
    },
    {
      titulo: '4. Uso aceptable',
      lista: [
        'No promocionar productos o servicios prohibidos por la ley peruana.',
        'No publicar publicidad engañosa, discriminatoria o que dañe a otras personas.',
        'No usar marcas, logos o imágenes de terceros sin permiso.',
        'No intentar vulnerar la seguridad de la plataforma ni usarla de forma automatizada para abusar del servicio.',
      ],
    },
    {
      titulo: '5. Suspensión de la cuenta',
      parrafos: [
        'Si una empresa incumple estos términos de forma grave o repetida, el equipo de AdBrand.AI puede suspender su cuenta. Te avisaremos por correo con el motivo y podrás escribirnos para pedir una revisión.',
      ],
    },
    {
      titulo: '6. Planes y pagos',
      parrafos: [
        'Hay un plan gratuito con un límite mensual de uso de IA y planes de pago con más capacidad. Los precios se muestran en soles (S/) antes de pagar.',
        'Los pagos con tarjeta y con Yape los procesa Mercado Pago. AdBrand.AI no guarda los números de tu tarjeta.',
      ],
    },
    {
      titulo: '7. Cambios en el servicio',
      parrafos: [
        'Podemos mejorar o cambiar funciones y estos términos. Si el cambio es importante, te avisaremos dentro de la plataforma o por correo antes de que entre en vigor.',
      ],
    },
    {
      titulo: '8. Contacto',
      parrafos: [`Para cualquier consulta sobre estos términos escríbenos a ${CORREO_CONTACTO}.`],
    },
  ],
}

export const PRIVACIDAD = {
  titulo: 'Política de privacidad',
  resumen: 'Qué datos guardamos, para qué los usamos y cómo puedes pedir verlos, corregirlos o borrarlos.',
  secciones: [
    {
      titulo: '1. Quién es responsable de tus datos',
      parrafos: [
        'AdBrand.AI trata los datos personales de las empresas que usan la plataforma, conforme a la Ley N.º 29733, Ley de Protección de Datos Personales, y su reglamento.',
      ],
    },
    {
      titulo: '2. Qué datos guardamos',
      lista: [
        'Datos de la cuenta: tu nombre, el nombre de tu empresa, tu correo y tu contraseña (guardada cifrada, nadie puede leerla).',
        'Datos del negocio que tú ingresas: rubro, productos, público, colores y logo.',
        'Los anuncios, kits de marca y publicaciones que creas en la plataforma.',
        'Datos de uso: cuánto usas la IA en el mes y las acciones importantes en tu cuenta, para seguridad.',
        'Datos de pago: el plan, el monto y el estado del pago. Los datos de tu tarjeta los recibe y protege Mercado Pago; nosotros no los vemos ni los guardamos.',
      ],
    },
    {
      titulo: '3. Para qué los usamos',
      lista: [
        'Darte el servicio: crear tus anuncios, tu kit de marca y tu calendario.',
        'Revisar que los anuncios cumplan las reglas (moderación).',
        'Avisarte por correo de cambios en tu cuenta o en tus anuncios.',
        'Cobrar tu plan y controlar el límite de uso de IA.',
        'Proteger la plataforma contra fraudes y accesos no autorizados.',
      ],
    },
    {
      titulo: '4. Con quién los compartimos',
      parrafos: [
        'No vendemos tus datos. Solo los compartimos con los servicios que necesitamos para funcionar: el proveedor de IA que genera los textos e imágenes y Mercado Pago para los pagos. Cada uno recibe solo lo necesario.',
      ],
    },
    {
      titulo: '5. Cuánto tiempo los guardamos',
      parrafos: [
        'Mientras tu cuenta esté activa. Si pides borrar tu cuenta, eliminamos tus datos, salvo los que la ley nos obliga a conservar (por ejemplo, los registros de pagos).',
      ],
    },
    {
      titulo: '6. Tus derechos',
      parrafos: [
        `Puedes pedir en cualquier momento ver, corregir o borrar tus datos, u oponerte a un uso en particular (derechos ARCO). Escríbenos a ${CORREO_CONTACTO} desde el correo de tu cuenta y te responderemos en un plazo máximo de 20 días hábiles.`,
        'Si no estás conforme con la respuesta, puedes acudir a la Autoridad Nacional de Protección de Datos Personales del Ministerio de Justicia y Derechos Humanos.',
      ],
    },
    {
      titulo: '7. Seguridad',
      parrafos: [
        'Usamos conexiones cifradas, contraseñas cifradas, sesiones que vencen y registros de auditoría para proteger tu información.',
      ],
    },
    {
      titulo: '8. Cookies y almacenamiento',
      parrafos: [
        'Solo guardamos en tu navegador lo necesario para mantener tu sesión abierta. No usamos cookies de publicidad ni de rastreo.',
      ],
    },
  ],
}
