import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  ArrowUpCircle,
  Banknote,
  Building2,
  Check,
  Clock,
  CreditCard,
  Inbox,
  RefreshCw,
} from "lucide-react";
import { toast } from "sonner";
import {
  realizarRetiro,
  usarCodigoRetiro,
  solicitarPrestamo,
  obtenerPrestamosPendientesDePrestamista,
  responderPrestamo,
  TransaccionResponse,
} from "@/services/transaccionService";
import { cn } from "@/lib/utils";

type Tab = "RETIRO" | "PRESTAMO";
type RetiroFase = "form" | "codigo" | "completo";

const RETIRO_KEY = "retiro_pendiente";

const DepositoRetiro = () => {
  const { user } = useAuth();
  const [tab, setTab] = useState<Tab>("RETIRO");

  // ── Retiro state ──────────────────────────────────────────────
  const [amount, setAmount] = useState("");
  const [retiroFase, setRetiroFase] = useState<RetiroFase>("form");
  const [codigo, setCodigo] = useState("");
  const [montoRetiro, setMontoRetiro] = useState(0);
  const [segundos, setSegundos] = useState(0);
  const [loadingRetiro, setLoadingRetiro] = useState(false);
  const [loadingCajero, setLoadingCajero] = useState(false);

  // ── Préstamo state ────────────────────────────────────────────
  const [cuentaPrestamista, setCuentaPrestamista] = useState("");
  const [montoPrestamo, setMontoPrestamo] = useState("");
  const [loadingPrestamo, setLoadingPrestamo] = useState(false);
  const [prestamoEnviado, setPrestamoEnviado] = useState(false);

  // ── Solicitudes recibidas (yo soy el prestamista) ─────────────
  const [solicitudesRecibidas, setSolicitudesRecibidas] = useState<
    TransaccionResponse[]
  >([]);
  const [loadingSolicitudes, setLoadingSolicitudes] = useState(false);
  const [respondiendo, setRespondiendo] = useState<number | null>(null);

  const cargarSolicitudes = useCallback(async () => {
    if (!user) return;
    setLoadingSolicitudes(true);
    try {
      const data = await obtenerPrestamosPendientesDePrestamista(
        user.numeroCuenta,
      );
      setSolicitudesRecibidas(data);
    } catch {
      // silencioso — puede no tener solicitudes
    } finally {
      setLoadingSolicitudes(false);
    }
  }, [user]);

  useEffect(() => {
    cargarSolicitudes();
  }, [cargarSolicitudes]);

  // Restaurar retiro pendiente desde localStorage al montar
  useEffect(() => {
    try {
      const stored = localStorage.getItem(RETIRO_KEY);
      if (stored) {
        const { codigo: c, montoRetiro: m, expiracion } = JSON.parse(stored);
        const segsRestantes = Math.max(
          0,
          Math.floor((new Date(expiracion).getTime() - Date.now()) / 1000),
        );
        if (segsRestantes > 0) {
          setCodigo(c);
          setMontoRetiro(m);
          setSegundos(segsRestantes);
          setRetiroFase("codigo");
        } else {
          localStorage.removeItem(RETIRO_KEY);
        }
      }
    } catch {
      localStorage.removeItem(RETIRO_KEY);
    }
  }, []);

  // Countdown timer
  useEffect(() => {
    if (retiroFase !== "codigo" || segundos <= 0) return;
    const timer = setInterval(() => {
      setSegundos((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [retiroFase, segundos]);

  if (!user) return null;

  const parsedAmount = parseFloat(amount) || 0;
  const parsedMonto = parseFloat(montoPrestamo) || 0;

  const formatTime = (s: number) =>
    `${Math.floor(s / 60)
      .toString()
      .padStart(2, "0")}:${(s % 60).toString().padStart(2, "0")}`;

  const resetRetiro = () => {
    setRetiroFase("form");
    setAmount("");
    setCodigo("");
    setSegundos(0);
    setMontoRetiro(0);
    localStorage.removeItem(RETIRO_KEY);
  };

  // ── Handlers ──────────────────────────────────────────────────
  const handleGenerarCodigo = async () => {
    if (parsedAmount <= 0) return;
    setLoadingRetiro(true);
    try {
      const res = await realizarRetiro(user.numeroCuenta, parsedAmount);
      if (res.codigoRetiro && res.fechaExpiracionCodigo) {
        const expMs = new Date(res.fechaExpiracionCodigo).getTime();
        const segs = Math.max(0, Math.floor((expMs - Date.now()) / 1000));
        setCodigo(res.codigoRetiro);
        setMontoRetiro(parsedAmount);
        setSegundos(segs || 600);
        setRetiroFase("codigo");
        // Persistir en localStorage
        localStorage.setItem(
          RETIRO_KEY,
          JSON.stringify({
            codigo: res.codigoRetiro,
            montoRetiro: parsedAmount,
            expiracion: res.fechaExpiracionCodigo,
          }),
        );
        toast.info(
          "Código generado. Tienes 10 minutos para usarlo en el cajero.",
        );
      }
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Error al generar el código",
      );
    } finally {
      setLoadingRetiro(false);
    }
  };

  const handleUsarCodigo = async () => {
    if (!codigo || segundos <= 0) return;
    setLoadingCajero(true);
    try {
      const res = await usarCodigoRetiro(codigo);
      setRetiroFase("completo");
      localStorage.removeItem(RETIRO_KEY);
      toast.success(
        `Retiro de $${res.monto?.toLocaleString("es-CO")} completado`,
      );
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Error al usar el código",
      );
    } finally {
      setLoadingCajero(false);
    }
  };

  const handleSolicitarPrestamo = async () => {
    if (!cuentaPrestamista.trim() || parsedMonto <= 0) return;
    setLoadingPrestamo(true);
    try {
      await solicitarPrestamo(
        cuentaPrestamista.trim(),
        user.numeroCuenta,
        parsedMonto,
      );
      setPrestamoEnviado(true);
      toast.success("Solicitud enviada. Quedará pendiente de aprobación.");
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Error al enviar la solicitud",
      );
    } finally {
      setLoadingPrestamo(false);
    }
  };

  const handleResponderPrestamo = async (
    id: number,
    estado: "APROBADA" | "RECHAZADA",
  ) => {
    setRespondiendo(id);
    try {
      await responderPrestamo(id, user.numeroCuenta, estado);
      toast.success(
        estado === "APROBADA" ? "Préstamo aprobado" : "Préstamo rechazado",
      );
      await cargarSolicitudes();
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "Error al responder la solicitud",
      );
    } finally {
      setRespondiendo(null);
    }
  };

  return (
    <div className="max-w-md mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">
          Retiro / Préstamo
        </h1>
        <p className="text-muted-foreground text-sm mt-1">
          Retira en cajero o solicita dinero prestado
        </p>
      </div>

      {/* Tab toggle */}
      <div className="grid grid-cols-2 gap-2 p-1 bg-muted rounded-lg">
        <button
          onClick={() => {
            setTab("RETIRO");
            resetRetiro();
          }}
          className={cn(
            "flex items-center justify-center gap-2 py-2.5 rounded-md text-sm font-medium transition-all",
            tab === "RETIRO"
              ? "bg-background shadow text-foreground"
              : "text-muted-foreground hover:text-foreground",
          )}
        >
          <ArrowUpCircle className="w-4 h-4 text-red-500" />
          Retiro Cajero
        </button>
        <button
          onClick={() => {
            setTab("PRESTAMO");
            setPrestamoEnviado(false);
          }}
          className={cn(
            "flex items-center justify-center gap-2 py-2.5 rounded-md text-sm font-medium transition-all",
            tab === "PRESTAMO"
              ? "bg-background shadow text-foreground"
              : "text-muted-foreground hover:text-foreground",
          )}
        >
          <Banknote className="w-4 h-4 text-blue-500" />
          Pedir Préstamo
        </button>
      </div>

      {/* ── RETIRO ── */}
      {tab === "RETIRO" && (
        <div className="bg-card border border-border rounded-xl p-6 space-y-5">
          {/* Fase 1: formulario */}
          {retiroFase === "form" && (
            <>
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">Cuenta</Label>
                <p className="font-mono text-sm font-medium bg-muted px-3 py-2 rounded-md">
                  {user.numeroCuenta}
                </p>
              </div>

              <div className="flex items-center justify-between bg-muted/50 rounded-md px-3 py-2">
                <span className="text-sm text-muted-foreground">
                  Saldo disponible
                </span>
                <span className="font-semibold text-sm">
                  ${user.saldo.toLocaleString("es-CO")}
                </span>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="amount">Monto a retirar</Label>
                <Input
                  id="amount"
                  type="number"
                  min="1"
                  max="5000000"
                  step="any"
                  placeholder="0"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="text-lg font-mono"
                />
                <p className="text-xs text-muted-foreground">
                  Máximo $5,000,000 por retiro en cajero
                </p>
                {parsedAmount > user.saldo && parsedAmount > 0 && (
                  <p className="text-xs text-destructive">Saldo insuficiente</p>
                )}
                {parsedAmount > 5_000_000 && (
                  <p className="text-xs text-destructive">
                    Supera el límite de retiro en cajero
                  </p>
                )}
              </div>

              <Button
                className="w-full"
                variant="destructive"
                disabled={
                  parsedAmount <= 0 ||
                  parsedAmount > user.saldo ||
                  parsedAmount > 5_000_000 ||
                  loadingRetiro
                }
                onClick={handleGenerarCodigo}
              >
                {loadingRetiro ? (
                  "Generando código..."
                ) : (
                  <span className="flex items-center gap-2">
                    <CreditCard className="w-4 h-4" /> Generar código
                  </span>
                )}
              </Button>
            </>
          )}

          {/* Fase 2: código generado */}
          {retiroFase === "codigo" && (
            <div className="space-y-5">
              <div className="text-center space-y-2">
                <p className="text-sm text-muted-foreground">
                  Tu código de retiro
                </p>
                <div className="flex justify-center gap-2 my-3">
                  {codigo.split("").map((digit, i) => (
                    <span
                      key={i}
                      className="w-10 h-12 flex items-center justify-center bg-muted rounded-lg text-2xl font-mono font-bold border border-border"
                    >
                      {digit}
                    </span>
                  ))}
                </div>
                <p className="text-xs text-muted-foreground">
                  Monto:{" "}
                  <span className="font-semibold text-foreground">
                    ${montoRetiro.toLocaleString("es-CO")}
                  </span>
                </p>
              </div>

              {/* Countdown */}
              <div
                className={cn(
                  "flex items-center justify-center gap-2 py-3 rounded-lg",
                  segundos > 120
                    ? "bg-green-50 dark:bg-green-950/20 text-green-700 dark:text-green-400"
                    : segundos > 0
                      ? "bg-amber-50 dark:bg-amber-950/20 text-amber-700 dark:text-amber-400"
                      : "bg-red-50 dark:bg-red-950/20 text-red-700 dark:text-red-400",
                )}
              >
                <Clock className="w-4 h-4" />
                <span className="font-mono text-xl font-bold">
                  {formatTime(segundos)}
                </span>
                <span className="text-xs">
                  {segundos > 0 ? "restantes" : "EXPIRADO"}
                </span>
              </div>

              <div className="space-y-2">
                <Button
                  className="w-full"
                  disabled={segundos <= 0 || loadingCajero}
                  onClick={handleUsarCodigo}
                >
                  <Building2 className="w-4 h-4 mr-2" />
                  {loadingCajero
                    ? "Procesando en cajero..."
                    : "Simular uso en cajero"}
                </Button>
                <Button
                  variant="ghost"
                  className="w-full text-xs text-muted-foreground"
                  onClick={resetRetiro}
                >
                  Cancelar
                </Button>
              </div>
            </div>
          )}

          {/* Fase 3: completado */}
          {retiroFase === "completo" && (
            <div className="text-center space-y-4 py-4">
              <div className="w-16 h-16 bg-green-100 dark:bg-green-950/30 rounded-full flex items-center justify-center mx-auto">
                <Check className="w-8 h-8 text-green-600" />
              </div>
              <div>
                <p className="font-semibold text-lg">¡Retiro completado!</p>
                <p className="text-sm text-muted-foreground">
                  El dinero fue entregado por el cajero
                </p>
              </div>
              <Button
                variant="outline"
                className="w-full"
                onClick={resetRetiro}
              >
                Nuevo retiro
              </Button>
            </div>
          )}
        </div>
      )}

      {/* ── PRÉSTAMO ── */}
      {tab === "PRESTAMO" && (
        <div className="bg-card border border-border rounded-xl p-6 space-y-5">
          {!prestamoEnviado ? (
            <>
              <div className="p-3 bg-blue-50 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                <p className="text-xs text-blue-700 dark:text-blue-300">
                  Solicita dinero prestado de otra cuenta. La solicitud quedará
                  pendiente hasta que sea aprobada por el administrador.
                </p>
              </div>

              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">
                  Tu cuenta (destino)
                </Label>
                <p className="font-mono text-sm font-medium bg-muted px-3 py-2 rounded-md">
                  {user.numeroCuenta}
                </p>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="cuentaPrestamista">
                  Cuenta del prestamista
                </Label>
                <Input
                  id="cuentaPrestamista"
                  placeholder="Ej: ACC-002"
                  value={cuentaPrestamista}
                  onChange={(e) => setCuentaPrestamista(e.target.value)}
                  className="font-mono"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="montoPrestamo">Monto a solicitar</Label>
                <Input
                  id="montoPrestamo"
                  type="number"
                  min="1"
                  step="any"
                  placeholder="0"
                  value={montoPrestamo}
                  onChange={(e) => setMontoPrestamo(e.target.value)}
                  className="text-lg font-mono"
                />
              </div>

              <Button
                className="w-full"
                disabled={
                  !cuentaPrestamista.trim() ||
                  parsedMonto <= 0 ||
                  loadingPrestamo
                }
                onClick={handleSolicitarPrestamo}
              >
                {loadingPrestamo ? (
                  "Enviando solicitud..."
                ) : (
                  <span className="flex items-center gap-2">
                    <Banknote className="w-4 h-4" /> Solicitar préstamo
                  </span>
                )}
              </Button>
            </>
          ) : (
            <div className="text-center space-y-4 py-4">
              <div className="w-16 h-16 bg-blue-100 dark:bg-blue-950/30 rounded-full flex items-center justify-center mx-auto">
                <Check className="w-8 h-8 text-blue-600" />
              </div>
              <div>
                <p className="font-semibold text-lg">¡Solicitud enviada!</p>
                <p className="text-sm text-muted-foreground">
                  El préstamo está pendiente de aprobación por el dueño de la
                  cuenta prestamista
                </p>
              </div>
              <Button
                variant="outline"
                className="w-full"
                onClick={() => {
                  setPrestamoEnviado(false);
                  setCuentaPrestamista("");
                  setMontoPrestamo("");
                }}
              >
                Nueva solicitud
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Solicitudes de préstamo recibidas (yo soy el prestamista) */}
      <div className="bg-card border border-border rounded-xl p-5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Inbox className="w-4 h-4 text-muted-foreground" />
            <span className="font-semibold text-sm">
              Solicitudes de préstamo recibidas
            </span>
            {solicitudesRecibidas.length > 0 && (
              <span className="bg-amber-500 text-white text-xs font-bold rounded-full px-2 py-0.5">
                {solicitudesRecibidas.length}
              </span>
            )}
          </div>
          <button
            onClick={cargarSolicitudes}
            disabled={loadingSolicitudes}
            className="text-muted-foreground hover:text-foreground transition-colors"
            title="Actualizar"
          >
            <RefreshCw
              className={cn("w-4 h-4", loadingSolicitudes && "animate-spin")}
            />
          </button>
        </div>

        {loadingSolicitudes ? (
          <p className="text-sm text-muted-foreground text-center py-2">
            Cargando...
          </p>
        ) : solicitudesRecibidas.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-2">
            No tienes solicitudes pendientes
          </p>
        ) : (
          <div className="space-y-3">
            {solicitudesRecibidas.map((s) => (
              <div
                key={s.id}
                className="border border-border rounded-lg p-3 space-y-3"
              >
                <div className="flex justify-between items-start text-sm">
                  <div>
                    <p className="text-muted-foreground text-xs">Solicitante</p>
                    <p className="font-mono font-medium">{s.cuentaDestinoId}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-muted-foreground text-xs">Monto</p>
                    <p className="font-bold text-base">
                      ${s.monto.toLocaleString("es-CO")}
                    </p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    className="flex-1 bg-green-600 hover:bg-green-700 text-white"
                    disabled={respondiendo === s.id}
                    onClick={() => handleResponderPrestamo(s.id, "APROBADA")}
                  >
                    {respondiendo === s.id ? "..." : "Aprobar"}
                  </Button>
                  <Button
                    size="sm"
                    variant="destructive"
                    className="flex-1"
                    disabled={respondiendo === s.id}
                    onClick={() => handleResponderPrestamo(s.id, "RECHAZADA")}
                  >
                    {respondiendo === s.id ? "..." : "Rechazar"}
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default DepositoRetiro;
