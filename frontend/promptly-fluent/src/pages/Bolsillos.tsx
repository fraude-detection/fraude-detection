import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  PiggyBank,
  Target,
  Shield,
  Plus,
  Trash2,
  ArrowDownCircle,
  ArrowUpCircle,
  RefreshCw,
  X,
  CheckCircle2,
} from "lucide-react";
import { toast } from "sonner";
import {
  listarBolsillos,
  crearBolsillo,
  consignarBolsillo,
  retirarBolsillo,
  eliminarBolsillo,
  Bolsillo,
} from "@/services/bolsilloService";
import { cn } from "@/lib/utils";

type TipoBolsillo = "BOLSILLO" | "META" | "COLCHON";

const TIPO_INFO: Record<
  TipoBolsillo,
  { label: string; color: string; bg: string; icon: React.ReactNode }
> = {
  BOLSILLO: {
    label: "Bolsillo",
    color: "text-purple-600",
    bg: "bg-purple-50 border-purple-200",
    icon: <PiggyBank className="w-5 h-5 text-purple-500" />,
  },
  META: {
    label: "Meta",
    color: "text-blue-600",
    bg: "bg-blue-50 border-blue-200",
    icon: <Target className="w-5 h-5 text-blue-500" />,
  },
  COLCHON: {
    label: "Colchón",
    color: "text-green-600",
    bg: "bg-green-50 border-green-200",
    icon: <Shield className="w-5 h-5 text-green-500" />,
  },
};

const fmt = (n: number) =>
  `$${n.toLocaleString("es-CO", { minimumFractionDigits: 0 })}`;

// ─── Modal para operaciones ──────────────────────────────────────────────────
interface MovimientoModalProps {
  bolsillo: Bolsillo;
  accion: "consignar" | "retirar";
  onClose: () => void;
  onDone: () => void;
  numDocumento: string;
}

const MovimientoModal = ({
  bolsillo,
  accion,
  onClose,
  onDone,
  numDocumento,
}: MovimientoModalProps) => {
  const [monto, setMonto] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    const val = parseFloat(monto);
    if (!val || val <= 0) return;
    setLoading(true);
    try {
      if (accion === "consignar") {
        await consignarBolsillo(numDocumento, bolsillo.id, val);
        toast.success(`${fmt(val)} agregados a "${bolsillo.nombre}"`);
      } else {
        await retirarBolsillo(numDocumento, bolsillo.id, val);
        toast.success(`${fmt(val)} retirados de "${bolsillo.nombre}"`);
      }
      onDone();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error en la operación");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-bold text-lg capitalize">
            {accion === "consignar" ? "Agregar dinero" : "Retirar dinero"}
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <p className="text-sm text-gray-500 mb-4">
          {accion === "consignar"
            ? "Se descontará de tu cuenta principal"
            : "Se abonará a tu cuenta principal"}
        </p>
        <p className="text-sm font-medium mb-1">
          Bolsillo: <span className="font-bold">{bolsillo.nombre}</span>
        </p>
        <p className="text-sm text-gray-500 mb-4">
          Saldo actual:{" "}
          <span className="font-semibold">{fmt(bolsillo.saldo)}</span>
        </p>
        <div className="space-y-2 mb-5">
          <Label>Monto</Label>
          <Input
            type="number"
            placeholder="0"
            value={monto}
            onChange={(e) => setMonto(e.target.value)}
            min="1"
          />
        </div>
        <Button
          className="w-full"
          onClick={handleSubmit}
          disabled={loading || !monto || parseFloat(monto) <= 0}
        >
          {loading
            ? "Procesando..."
            : accion === "consignar"
              ? "Agregar"
              : "Retirar"}
        </Button>
      </div>
    </div>
  );
};

// ─── Modal crear bolsillo ────────────────────────────────────────────────────
interface CrearModalProps {
  tipo: TipoBolsillo;
  onClose: () => void;
  onDone: () => void;
  numDocumento: string;
  tieneColchon: boolean;
}

const CrearModal = ({
  tipo,
  onClose,
  onDone,
  numDocumento,
  tieneColchon,
}: CrearModalProps) => {
  const [nombre, setNombre] = useState(tipo === "COLCHON" ? "Mi Colchón" : "");
  const [descripcion, setDescripcion] = useState("");
  const [montoObjetivo, setMontoObjetivo] = useState("");
  const [fechaLimite, setFechaLimite] = useState("");
  const [loading, setLoading] = useState(false);
  const info = TIPO_INFO[tipo];

  const handleSubmit = async () => {
    if (!nombre.trim()) return;
    if (tipo === "COLCHON" && tieneColchon) {
      toast.error("Ya tienes un Colchón creado");
      return;
    }
    setLoading(true);
    try {
      await crearBolsillo(numDocumento, {
        nombre: nombre.trim(),
        descripcion: descripcion.trim() || undefined,
        tipo,
        montoObjetivo: montoObjetivo ? parseFloat(montoObjetivo) : undefined,
        fechaLimite: fechaLimite || undefined,
      });
      toast.success(`${info.label} "${nombre}" creado`);
      onDone();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al crear");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            {info.icon}
            <h3 className="font-bold text-lg">Nuevo {info.label}</h3>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="space-y-3">
          <div>
            <Label>Nombre *</Label>
            <Input
              placeholder={`Ej: ${tipo === "META" ? "Viaje a Cartagena" : tipo === "COLCHON" ? "Mi Colchón" : "Ropa"}`}
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              disabled={tipo === "COLCHON"}
            />
          </div>
          <div>
            <Label>Descripción (opcional)</Label>
            <Input
              placeholder="¿Para qué es?"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
            />
          </div>
          {(tipo === "META" || tipo === "BOLSILLO") && (
            <div>
              <Label>Monto objetivo (opcional)</Label>
              <Input
                type="number"
                placeholder="0"
                value={montoObjetivo}
                onChange={(e) => setMontoObjetivo(e.target.value)}
              />
            </div>
          )}
          {tipo === "META" && (
            <div>
              <Label>Fecha límite (opcional)</Label>
              <Input
                type="date"
                value={fechaLimite}
                onChange={(e) => setFechaLimite(e.target.value)}
                min={new Date().toISOString().split("T")[0]}
              />
            </div>
          )}
        </div>
        <Button
          className="w-full mt-5"
          onClick={handleSubmit}
          disabled={loading || !nombre.trim()}
        >
          {loading ? "Creando..." : `Crear ${info.label}`}
        </Button>
      </div>
    </div>
  );
};

// ─── Tarjeta de bolsillo ─────────────────────────────────────────────────────
interface BolsilloCardProps {
  bolsillo: Bolsillo;
  onConsignar: () => void;
  onRetirar: () => void;
  onEliminar: () => void;
}

const BolsilloCard = ({
  bolsillo,
  onConsignar,
  onRetirar,
  onEliminar,
}: BolsilloCardProps) => {
  const tipo = bolsillo.tipo as TipoBolsillo;
  const info = TIPO_INFO[tipo] || TIPO_INFO.BOLSILLO;
  const progreso =
    bolsillo.montoObjetivo && bolsillo.montoObjetivo > 0
      ? Math.min(100, (bolsillo.saldo / bolsillo.montoObjetivo) * 100)
      : null;

  return (
    <div className={cn("rounded-2xl border p-4 space-y-3", info.bg)}>
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-2">
          {info.icon}
          <div>
            <div className="flex items-center gap-2">
              <p className="font-bold text-sm">{bolsillo.nombre}</p>
              {bolsillo.completado && (
                <CheckCircle2 className="w-4 h-4 text-green-500" />
              )}
            </div>
            {bolsillo.descripcion && (
              <p className="text-xs text-gray-500">{bolsillo.descripcion}</p>
            )}
          </div>
        </div>
        <span
          className={cn(
            "text-xs font-semibold px-2 py-0.5 rounded-full",
            info.color,
            "bg-white/70",
          )}
        >
          {info.label}
        </span>
      </div>

      <div>
        <p className="text-2xl font-bold">{fmt(bolsillo.saldo)}</p>
        {bolsillo.montoObjetivo && (
          <p className="text-xs text-gray-500">
            Meta: {fmt(bolsillo.montoObjetivo)}
            {bolsillo.fechaLimite &&
              ` · hasta ${new Date(bolsillo.fechaLimite).toLocaleDateString("es-CO")}`}
          </p>
        )}
      </div>

      {progreso !== null && (
        <div>
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>{progreso.toFixed(0)}% completado</span>
            {bolsillo.completado && (
              <span className="text-green-600 font-semibold">
                ¡Meta alcanzada!
              </span>
            )}
          </div>
          <div className="h-2 bg-white/60 rounded-full overflow-hidden">
            <div
              className="h-full bg-current rounded-full transition-all"
              style={{
                width: `${progreso}%`,
                color:
                  tipo === "META"
                    ? "#3b82f6"
                    : tipo === "COLCHON"
                      ? "#22c55e"
                      : "#a855f7",
              }}
            />
          </div>
        </div>
      )}

      <div className="flex gap-2 pt-1">
        <Button
          size="sm"
          variant="outline"
          className="flex-1 text-xs bg-white/70 hover:bg-white"
          onClick={onConsignar}
        >
          <ArrowDownCircle className="w-3 h-3 mr-1" />
          Agregar
        </Button>
        <Button
          size="sm"
          variant="outline"
          className="flex-1 text-xs bg-white/70 hover:bg-white"
          onClick={onRetirar}
          disabled={bolsillo.saldo <= 0}
        >
          <ArrowUpCircle className="w-3 h-3 mr-1" />
          Retirar
        </Button>
        <Button
          size="sm"
          variant="outline"
          className="text-xs bg-white/70 hover:bg-red-50 hover:text-red-600 hover:border-red-300"
          onClick={onEliminar}
        >
          <Trash2 className="w-3 h-3" />
        </Button>
      </div>
    </div>
  );
};

// ─── Página principal ────────────────────────────────────────────────────────
const BolsillosPage = () => {
  const { user } = useAuth();
  const [bolsillos, setBolsillos] = useState<Bolsillo[]>([]);
  const [loading, setLoading] = useState(true);

  const [crearTipo, setCrearTipo] = useState<TipoBolsillo | null>(null);
  const [movimiento, setMovimiento] = useState<{
    bolsillo: Bolsillo;
    accion: "consignar" | "retirar";
  } | null>(null);

  const cargar = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await listarBolsillos(user.numDocumento);
      setBolsillos(data);
    } catch {
      toast.error("Error al cargar bolsillos");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  if (!user) return null;

  const tieneColchon = bolsillos.some((b) => b.tipo === "COLCHON");
  const bolsillosNormales = bolsillos.filter((b) => b.tipo === "BOLSILLO");
  const metas = bolsillos.filter((b) => b.tipo === "META");
  const colchon = bolsillos.find((b) => b.tipo === "COLCHON");

  const handleEliminar = async (b: Bolsillo) => {
    if (
      !window.confirm(
        `¿Eliminar "${b.nombre}"?${b.saldo > 0 ? ` El saldo de ${fmt(b.saldo)} se devolverá a tu cuenta.` : ""}`,
      )
    )
      return;
    try {
      await eliminarBolsillo(user.numDocumento, b.id);
      toast.success("Bolsillo eliminado");
      cargar();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al eliminar");
    }
  };

  return (
    <div className="p-4 max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Mis Bolsillos</h1>
          <p className="text-sm text-gray-500">
            Organiza tu dinero en bolsillos, metas y colchón
          </p>
        </div>
        <button
          onClick={cargar}
          disabled={loading}
          className="p-2 rounded-full hover:bg-gray-100"
        >
          <RefreshCw className={cn("w-4 h-4", loading && "animate-spin")} />
        </button>
      </div>

      {/* Crear nuevos */}
      <div className="grid grid-cols-3 gap-3">
        {(["BOLSILLO", "META", "COLCHON"] as TipoBolsillo[]).map((tipo) => {
          const info = TIPO_INFO[tipo];
          const disabled = tipo === "COLCHON" && tieneColchon;
          return (
            <button
              key={tipo}
              onClick={() => setCrearTipo(tipo)}
              disabled={disabled}
              className={cn(
                "flex flex-col items-center gap-2 p-4 rounded-2xl border-2 border-dashed transition-all",
                disabled
                  ? "opacity-40 cursor-not-allowed border-gray-200"
                  : "hover:border-solid hover:shadow-sm cursor-pointer border-gray-300 hover:bg-gray-50",
              )}
            >
              {info.icon}
              <span className="text-xs font-semibold text-gray-600">
                {disabled ? `Ya tienes ${info.label}` : `+ ${info.label}`}
              </span>
            </button>
          );
        })}
      </div>

      {loading && (
        <div className="text-center py-8 text-gray-400">Cargando...</div>
      )}

      {/* Colchón */}
      {colchon && (
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
            Colchón de emergencias
          </h2>
          <BolsilloCard
            bolsillo={colchon}
            onConsignar={() =>
              setMovimiento({ bolsillo: colchon, accion: "consignar" })
            }
            onRetirar={() =>
              setMovimiento({ bolsillo: colchon, accion: "retirar" })
            }
            onEliminar={() => handleEliminar(colchon)}
          />
        </section>
      )}

      {/* Metas */}
      {metas.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
            Metas de ahorro
          </h2>
          <div className="grid gap-3">
            {metas.map((b) => (
              <BolsilloCard
                key={b.id}
                bolsillo={b}
                onConsignar={() =>
                  setMovimiento({ bolsillo: b, accion: "consignar" })
                }
                onRetirar={() =>
                  setMovimiento({ bolsillo: b, accion: "retirar" })
                }
                onEliminar={() => handleEliminar(b)}
              />
            ))}
          </div>
        </section>
      )}

      {/* Bolsillos normales */}
      {bolsillosNormales.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
            Bolsillos
          </h2>
          <div className="grid gap-3">
            {bolsillosNormales.map((b) => (
              <BolsilloCard
                key={b.id}
                bolsillo={b}
                onConsignar={() =>
                  setMovimiento({ bolsillo: b, accion: "consignar" })
                }
                onRetirar={() =>
                  setMovimiento({ bolsillo: b, accion: "retirar" })
                }
                onEliminar={() => handleEliminar(b)}
              />
            ))}
          </div>
        </section>
      )}

      {!loading && bolsillos.length === 0 && (
        <div className="text-center py-12 text-gray-400">
          <PiggyBank className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p className="font-medium">Aún no tienes bolsillos</p>
          <p className="text-sm">Crea uno arriba para empezar a ahorrar</p>
        </div>
      )}

      {/* Modales */}
      {crearTipo && (
        <CrearModal
          tipo={crearTipo}
          numDocumento={user.numDocumento}
          tieneColchon={tieneColchon}
          onClose={() => setCrearTipo(null)}
          onDone={() => {
            setCrearTipo(null);
            cargar();
          }}
        />
      )}

      {movimiento && (
        <MovimientoModal
          bolsillo={movimiento.bolsillo}
          accion={movimiento.accion}
          numDocumento={user.numDocumento}
          onClose={() => setMovimiento(null)}
          onDone={() => {
            setMovimiento(null);
            cargar();
          }}
        />
      )}
    </div>
  );
};

export default BolsillosPage;
