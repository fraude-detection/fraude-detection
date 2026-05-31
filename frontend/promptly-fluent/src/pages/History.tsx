import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { StatusBadge } from "@/components/StatusBadge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  obtenerHistorial,
  TransaccionResponse,
} from "@/services/transaccionService";
import { toast } from "sonner";
import { ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";

type SortDir = "desc" | "asc";

const HistoryPage = () => {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState<TransaccionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [sortDir, setSortDir] = useState<SortDir>("desc");

  useEffect(() => {
    const cargarHistorial = async () => {
      if (!user) return;
      try {
        setLoading(true);
        const historial = await obtenerHistorial(user.numeroCuenta);
        setTransactions(historial);
      } catch (error) {
        toast.error("Error al cargar el historial");
        console.error(error);
      } finally {
        setLoading(false);
      }
    };

    cargarHistorial();
  }, [user]);

  if (!user) return null;

  const sorted = [...transactions].sort((a, b) => {
    const ta = a.fechaCreacion ? new Date(a.fechaCreacion).getTime() : 0;
    const tb = b.fechaCreacion ? new Date(b.fechaCreacion).getTime() : 0;
    return sortDir === "desc" ? tb - ta : ta - tb;
  });

  const mapearEstado = (
    estadoNombre?: string,
  ): "approved" | "rejected" | "pending" => {
    if (estadoNombre === "APROBADA") return "approved";
    if (estadoNombre === "RECHAZADA") return "rejected";
    return "pending";
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">
          Historial de Transferencias
        </h1>
        <p className="text-muted-foreground text-sm mt-1">
          {transactions.length} transacciones encontradas
        </p>
      </div>
      <div className="bg-card rounded-2xl shadow-card overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-muted-foreground">
            Cargando historial...
          </div>
        ) : transactions.length === 0 ? (
          <div className="p-8 text-center text-muted-foreground">
            No hay transacciones
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="border-border hover:bg-transparent">
                <TableHead
                  className="text-muted-foreground font-semibold text-xs uppercase tracking-wider cursor-pointer select-none hover:text-foreground transition-colors"
                  onClick={() =>
                    setSortDir((d) => (d === "desc" ? "asc" : "desc"))
                  }
                >
                  <span className="flex items-center gap-1">
                    Fecha
                    {sortDir === "desc" ? (
                      <ArrowDown className="w-3 h-3" />
                    ) : (
                      <ArrowUp className="w-3 h-3" />
                    )}
                  </span>
                </TableHead>
                <TableHead className="text-muted-foreground font-semibold text-xs uppercase tracking-wider">
                  De / Para
                </TableHead>
                <TableHead className="text-muted-foreground font-semibold text-xs uppercase tracking-wider">
                  Tipo
                </TableHead>
                <TableHead className="text-muted-foreground font-semibold text-xs uppercase tracking-wider text-right">
                  Monto
                </TableHead>
                <TableHead className="text-muted-foreground font-semibold text-xs uppercase tracking-wider text-center">
                  Estado
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sorted.map((txn) => (
                <TableRow key={txn.id} className="border-border">
                  <TableCell className="text-sm text-foreground">
                    {txn.fechaCreacion
                      ? new Date(txn.fechaCreacion).toLocaleDateString("es-ES")
                      : "-"}
                  </TableCell>
                  <TableCell className="text-sm text-foreground">
                    <span
                      className={
                        txn.cuentaOrigenId === user.numeroCuenta
                          ? "text-red-500"
                          : "text-green-500"
                      }
                    >
                      {txn.cuentaOrigenId === user.numeroCuenta
                        ? `→ ${txn.cuentaDestinoId}`
                        : `← ${txn.cuentaOrigenId}`}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-foreground">
                    <span className="inline-flex items-center rounded-full bg-secondary px-2 py-0.5 text-xs font-medium">
                      {txn.tipoTransaccionNombre ?? "-"}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm font-bold text-foreground text-right">
                    $
                    {txn.monto.toLocaleString("es-MX", {
                      minimumFractionDigits: 2,
                    })}
                  </TableCell>
                  <TableCell className="text-center">
                    <StatusBadge status={mapearEstado(txn.estadoNombre)} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>
    </div>
  );
};

export default HistoryPage;
