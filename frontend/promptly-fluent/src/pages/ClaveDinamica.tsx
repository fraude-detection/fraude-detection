import { useState, useEffect, useRef, useCallback } from "react";
import { useAuth } from "@/hooks/useAuth";
import {
  generarClave,
  ClaveDinamicaResponse,
} from "@/services/claveDinamicaService";
import { Button } from "@/components/ui/button";
import { Shield, RefreshCw, Copy, CheckCheck, Clock } from "lucide-react";
import { toast } from "@/hooks/use-toast";

const TOTAL_SECS = 5 * 60; // 5 minutos

export default function ClaveDinamicaPage() {
  const { user } = useAuth();
  const [clave, setClave] = useState<ClaveDinamicaResponse | null>(null);
  const [segundosRestantes, setSegundosRestantes] = useState(0);
  const [loading, setLoading] = useState(false);
  const [copiado, setCopiado] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const limpiarInterval = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  const iniciarContador = useCallback((expiracion: string) => {
    limpiarInterval();
    const calcSecs = () => {
      const diff = Math.floor(
        (new Date(expiracion).getTime() - Date.now()) / 1000,
      );
      return Math.max(0, diff);
    };
    setSegundosRestantes(calcSecs());
    intervalRef.current = setInterval(() => {
      const s = calcSecs();
      setSegundosRestantes(s);
      if (s === 0) limpiarInterval();
    }, 1000);
  }, []);

  useEffect(() => () => limpiarInterval(), []);

  const generar = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await generarClave(user.numDocumento);
      // El backend devuelve LocalDateTime como array o string, normalizar
      const expStr = Array.isArray(data.expiracion)
        ? new Date(
            data.expiracion[0],
            data.expiracion[1] - 1,
            data.expiracion[2],
            data.expiracion[3],
            data.expiracion[4],
            data.expiracion[5],
          ).toISOString()
        : data.expiracion;
      setClave({ ...data, expiracion: expStr });
      iniciarContador(expStr);
    } catch {
      toast({
        title: "Error",
        description: "No se pudo generar la clave",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const copiar = () => {
    if (!clave) return;
    navigator.clipboard.writeText(clave.codigo);
    setCopiado(true);
    setTimeout(() => setCopiado(false), 2000);
    toast({ title: "Copiado", description: "Clave copiada al portapapeles" });
  };

  const expirada = segundosRestantes === 0 && clave !== null;
  const mins = Math.floor(segundosRestantes / 60);
  const secs = segundosRestantes % 60;
  const progreso = (segundosRestantes / TOTAL_SECS) * 100;

  // Color del arco según tiempo restante
  const colorArco =
    segundosRestantes > 120
      ? "#22c55e"
      : segundosRestantes > 60
        ? "#eab308"
        : "#ef4444";

  // SVG circular countdown
  const radio = 54;
  const circunferencia = 2 * Math.PI * radio;
  const dashoffset = circunferencia * (1 - progreso / 100);

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] p-6">
      <div className="w-full max-w-sm space-y-6">
        {/* Header */}
        <div className="text-center space-y-1">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-primary/10 mb-2">
            <Shield className="w-7 h-7 text-primary" />
          </div>
          <h1 className="text-2xl font-bold">Clave Dinámica</h1>
          <p className="text-sm text-muted-foreground">
            Código temporal de un solo uso · válido 5 minutos
          </p>
        </div>

        {/* Card principal */}
        <div className="rounded-2xl border bg-card shadow-sm p-6 space-y-6">
          {clave === null ? (
            /* Estado inicial */
            <div className="flex flex-col items-center gap-4 py-4">
              <p className="text-center text-sm text-muted-foreground">
                Genera tu clave para autorizar una operación.
              </p>
              <Button
                onClick={generar}
                disabled={loading}
                className="w-full gap-2"
              >
                {loading ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <Shield className="w-4 h-4" />
                )}
                Generar clave
              </Button>
            </div>
          ) : (
            <>
              {/* Círculo de countdown */}
              <div className="flex justify-center">
                <div className="relative w-32 h-32">
                  <svg
                    className="w-full h-full -rotate-90"
                    viewBox="0 0 120 120"
                  >
                    {/* Pista */}
                    <circle
                      cx="60"
                      cy="60"
                      r={radio}
                      fill="none"
                      stroke="#e5e7eb"
                      strokeWidth="8"
                    />
                    {/* Progreso */}
                    <circle
                      cx="60"
                      cy="60"
                      r={radio}
                      fill="none"
                      stroke={expirada ? "#d1d5db" : colorArco}
                      strokeWidth="8"
                      strokeLinecap="round"
                      strokeDasharray={circunferencia}
                      strokeDashoffset={expirada ? circunferencia : dashoffset}
                      style={{
                        transition: "stroke-dashoffset 1s linear, stroke 0.5s",
                      }}
                    />
                  </svg>
                  {/* Tiempo en el centro */}
                  <div className="absolute inset-0 flex flex-col items-center justify-center">
                    <Clock className="w-4 h-4 text-muted-foreground mb-0.5" />
                    {expirada ? (
                      <span className="text-xs font-medium text-destructive">
                        Expirada
                      </span>
                    ) : (
                      <span className="text-lg font-bold tabular-nums">
                        {mins}:{String(secs).padStart(2, "0")}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Dígitos */}
              <div className="flex justify-center gap-2">
                {clave.codigo.split("").map((d, i) => (
                  <div
                    key={i}
                    className={`w-11 h-14 flex items-center justify-center rounded-xl text-2xl font-bold border-2 transition-all
                      ${
                        expirada
                          ? "bg-muted text-muted-foreground border-muted"
                          : "bg-primary/5 border-primary/30 text-foreground"
                      }`}
                  >
                    {expirada ? "•" : d}
                  </div>
                ))}
              </div>

              {/* Acciones */}
              {!expirada ? (
                <Button
                  variant="outline"
                  onClick={copiar}
                  className="w-full gap-2"
                >
                  {copiado ? (
                    <>
                      <CheckCheck className="w-4 h-4 text-green-500" /> Copiado
                    </>
                  ) : (
                    <>
                      <Copy className="w-4 h-4" /> Copiar clave
                    </>
                  )}
                </Button>
              ) : null}

              <Button
                onClick={generar}
                disabled={loading}
                variant={expirada ? "default" : "ghost"}
                className="w-full gap-2"
              >
                {loading ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <RefreshCw className="w-4 h-4" />
                )}
                {expirada ? "Generar nueva clave" : "Regenerar"}
              </Button>
            </>
          )}
        </div>

        {/* Info */}
        <p className="text-xs text-center text-muted-foreground px-4">
          Esta clave es de un solo uso. No la compartas con nadie, ni siquiera
          con el banco.
        </p>
      </div>
    </div>
  );
}
