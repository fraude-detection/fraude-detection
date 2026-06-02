// src/services/transaccionService.ts

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "https://fraude-detection-backend.onrender.com";
const API_URL = `${BASE_URL}/api/transacciones`;
const CATALOG_URL = `${BASE_URL}/api/catalogos`;

export interface TipoTransaccion {
  id: number;
  nombre: string;
  descripcion?: string;
}

export interface EstadoTransaccion {
  id: number;
  nombre: string;
  descripcion?: string;
}

export interface Transaccion {
  id?: number;
  monto: number;
  cuentaOrigenId: string;
  cuentaDestinoId: string;
  estadoId?: number;
  estadoNombre?: string;
  estadoTransaccion?: EstadoTransaccion;
  tipoTransaccion?: TipoTransaccion;
  tipoTransaccionNombre?: string;
  fechaCreacion?: string;
}

export interface TransaccionResponse extends Transaccion {
  id: number;
  estadoId: number;
  estadoNombre: string;
  estado?: string;
  codigoRetiro?: string;
  fechaExpiracionCodigo?: string;
}

export const obtenerTiposTransaccion = async (): Promise<TipoTransaccion[]> => {
  const response = await fetch(`${CATALOG_URL}/tipos-transaccion`);
  if (!response.ok) throw new Error("Error al obtener tipos de transacción");
  return response.json();
};

export const obtenerEstadosTransaccion = async (): Promise<EstadoTransaccion[]> => {
  const response = await fetch(`${CATALOG_URL}/estados-transaccion`);
  if (!response.ok) throw new Error("Error al obtener estados de transacción");
  return response.json();
};


const parseError = async (response: Response, fallback: string): Promise<never> => {
  let message = fallback;
  try {
    const errorData = await response.json();
    if (errorData?.message) {
      message = errorData.message;
    }
  } catch {
    // Si no hay body JSON válido, se usa el mensaje por defecto.
  }

  throw new Error(message);
};

export const crearTransaccion = async (transaccion: Transaccion): Promise<TransaccionResponse> => {
  try {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(transaccion),
    });

    if (!response.ok) {
      await parseError(response, "Error al crear transacción");
    }

    return await response.json();
  } catch (error) {
    throw error;
  }
};

export const obtenerHistorial = async (numeroCuenta: string): Promise<TransaccionResponse[]> => {
  try {
    const response = await fetch(`${API_URL}/cuenta/${numeroCuenta}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      await parseError(response, "Error al obtener historial");
    }

    return await response.json();
  } catch (error) {
    throw error;
  }
};

export const obtenerTodasTransacciones = async (adminDocumento: string): Promise<TransaccionResponse[]> => {
  const response = await fetch(API_URL, {
    headers: {
      "Content-Type": "application/json",
      "X-Admin-Documento": adminDocumento,
    },
  });

  if (!response.ok) {
    await parseError(response, "Error al obtener transacciones");
  }

  return response.json();
};

export const obtenerTransaccionesPendientes = async (adminDocumento: string): Promise<TransaccionResponse[]> => {
  const response = await fetch(`${API_URL}/pendientes`, {
    headers: {
      "Content-Type": "application/json",
      "X-Admin-Documento": adminDocumento,
    },
  });

  if (!response.ok) {
    await parseError(response, "Error al obtener transacciones pendientes");
  }

  return response.json();
};

export const actualizarEstadoTransaccion = async (
  id: number,
  estadoNombre: "APROBADA" | "RECHAZADA",
  adminDocumento: string,
): Promise<TransaccionResponse> => {
  const response = await fetch(`${API_URL}/${id}/estado`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-Admin-Documento": adminDocumento,
    },
    body: JSON.stringify({ estadoNombre }),
  });

  if (!response.ok) {
    await parseError(response, "Error al actualizar estado de transacción");
  }

  return response.json();
};

export const realizarDeposito = async (
  numeroCuenta: string,
  monto: number,
): Promise<TransaccionResponse> => {
  const response = await fetch(`${API_URL}/deposito`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ numeroCuenta, monto }),
  });
  if (!response.ok) {
    await parseError(response, "Error al procesar el depósito");
  }
  return response.json();
};

export const realizarRetiro = async (
  numeroCuenta: string,
  monto: number,
): Promise<TransaccionResponse> => {
  const response = await fetch(`${API_URL}/retiro`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ numeroCuenta, monto }),
  });
  if (!response.ok) {
    await parseError(response, "Error al procesar el retiro");
  }
  return response.json();
};

export const usarCodigoRetiro = async (
  codigo: string,
): Promise<TransaccionResponse> => {
  const response = await fetch(`${API_URL}/retiro/usar-codigo`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ codigo }),
  });
  if (!response.ok) {
    await parseError(response, "Error al usar el código de retiro");
  }
  return response.json();
};

export const solicitarPrestamo = async (
  cuentaOrigen: string,
  cuentaDestino: string,
  monto: number,
): Promise<TransaccionResponse> => {
  const response = await fetch(`${API_URL}/prestamo`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ cuentaOrigen, cuentaDestino, monto }),
  });
  if (!response.ok) {
    await parseError(response, "Error al solicitar el préstamo");
  }
  return response.json();
};

export const obtenerPrestamosPendientesDePrestamista = async (
  numeroCuenta: string,
): Promise<TransaccionResponse[]> => {
  const response = await fetch(
    `${API_URL}/prestamos-pendientes/${numeroCuenta}`,
  );
  if (!response.ok) {
    await parseError(response, "Error al obtener préstamos pendientes");
  }
  return response.json();
};

export const responderPrestamo = async (
  id: number,
  numeroCuenta: string,
  estado: "APROBADA" | "RECHAZADA",
): Promise<TransaccionResponse> => {
  const response = await fetch(`${API_URL}/prestamo/${id}/responder`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-Numero-Cuenta": numeroCuenta,
    },
    body: JSON.stringify({ estado }),
  });
  if (!response.ok) {
    await parseError(response, "Error al responder el préstamo");
  }
  return response.json();
};
