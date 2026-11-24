// Model: carga una sola vez el SDK oficial de Mercado Pago (MercadoPago.js v2).
// El SDK dibuja los campos de la tarjeta y la convierte en un token: el número y el CVV
// nunca pasan por nuestro código ni por nuestro servidor.
const URL_SDK = 'https://sdk.mercadopago.com/js/v2'

let promesaSdk = null

export function cargarSdkMercadoPago() {
  if (typeof window !== 'undefined' && window.MercadoPago) return Promise.resolve(window.MercadoPago)
  if (promesaSdk) return promesaSdk

  promesaSdk = new Promise((resolver, rechazar) => {
    const script = document.createElement('script')
    script.src = URL_SDK
    script.async = true
    script.onload = () => (window.MercadoPago ? resolver(window.MercadoPago) : rechazar(new Error('SDK sin cargar')))
    script.onerror = () => {
      promesaSdk = null
      rechazar(new Error('No se pudo cargar Mercado Pago'))
    }
    document.head.appendChild(script)
  })
  return promesaSdk
}
