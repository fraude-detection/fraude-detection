const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const API = `${BASE_URL}/api/bolsillos`;

export interface Bolsillo {
  id: number;
  numDocumento: string;
  nombre: string;
  descripcion?: string;
  tipo: "BOLSILLO" | "META" | "COLCHON";
  saldo: number;
  montoObjetivo?: number;
  fechaLimite?: string;
  completado: boolean;
  fechaCreacion: string;
}

export interface BolsilloRequest {
  nombre: string;
  descripcion?: string;
  tipo: "BOLSILLO" | "META" | "COLCHON";
  montoObjetivo?: number;
  fechaLimite?: string;
}

function headers(numDocumento: string) {
  return {
    "Content-Type": "application/json",
    "X-Num-Documento": numDocumento,
  };
}

async function handleResponse<T>(res: Response): Promise<T> {
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || "Error en la operación");
  return data as T;
}

export const listarBolsillos = (numDocumento: string): Promise<Bolsillo[]> =>
  fetch(API, { headers: headers(numDocumento) }).then((r) =>
    handleResponse<Bolsillo[]>(r),
  );

export const crearBolsillo = (
  numDocumento: string,
  request: BolsilloRequest,
): Promise<Bolsillo> =>
  fetch(API, {
    method: "POST",
    headers: headers(numDocumento),
    body: JSON.stringify(request),
  }).then((r) => handleResponse<Bolsillo>(r));

export const consignarBolsillo = (
  numDocumento: string,
  id: number,
  monto: number,
): Promise<Bolsillo> =>
  fetch(`${API}/${id}/consignar`, {
    method: "POST",
    headers: headers(numDocumento),
    body: JSON.stringify({ monto }),
  }).then((r) => handleResponse<Bolsillo>(r));

export const retirarBolsillo = (
  numDocumento: string,
  id: number,
  monto: number,
): Promise<Bolsillo> =>
  fetch(`${API}/${id}/retirar`, {
    method: "POST",
    headers: headers(numDocumento),
    body: JSON.stringify({ monto }),
  }).then((r) => handleResponse<Bolsillo>(r));

export const eliminarBolsillo = (
  numDocumento: string,
  id: number,
): Promise<void> =>
  fetch(`${API}/${id}`, {
    method: "DELETE",
    headers: headers(numDocumento),
  }).then((r) => handleResponse<void>(r));
