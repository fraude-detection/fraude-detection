const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "https://fraude-detection-backend.onrender.com";
const API = `${BASE_URL}/api/clave-dinamica`;

export interface ClaveDinamicaResponse {
  codigo: string;
  expiracion: string;
  minutosVigencia: number;
}

export interface ValidarResponse {
  valida: boolean;
  mensaje: string;
}

export const generarClave = async (numDocumento: string): Promise<ClaveDinamicaResponse> => {
  const res = await fetch(`${API}/generar`, {
    method: "POST",
    headers: { "X-Num-Documento": numDocumento },
  });
  if (!res.ok) throw new Error("Error al generar clave dinámica");
  return res.json();
};

export const validarClave = async (numDocumento: string, codigo: string): Promise<ValidarResponse> => {
  const res = await fetch(`${API}/validar`, {
    method: "POST",
    headers: {
      "X-Num-Documento": numDocumento,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ codigo }),
  });
  if (!res.ok) throw new Error("Error al validar clave dinámica");
  return res.json();
};
