import { useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { UserCircle, Eye, EyeOff, Save, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";

const PerfilPage = () => {
  const { user, updateUser } = useAuth();

  const [nombre, setNombre] = useState(
    user?.nombreCompleto?.split(" ")[0] ?? "",
  );
  const [apellido, setApellido] = useState(
    user?.nombreCompleto?.split(" ").slice(1).join(" ") ?? "",
  );
  const [email, setEmail] = useState(user?.username ?? "");
  const [passwordActual, setPasswordActual] = useState("");
  const [passwordNuevo, setPasswordNuevo] = useState("");
  const [showActual, setShowActual] = useState(false);
  const [showNuevo, setShowNuevo] = useState(false);
  const [loading, setLoading] = useState(false);
  const [guardado, setGuardado] = useState(false);

  if (!user) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (passwordNuevo && !passwordActual) {
      toast.error("Debes ingresar tu contraseña actual para cambiarla");
      return;
    }

    setLoading(true);
    try {
      const body: Record<string, string> = {};
      if (nombre.trim()) body.nombre = nombre.trim();
      if (apellido.trim()) body.apellido = apellido.trim();
      if (email.trim()) body.email = email.trim();
      if (passwordActual) body.passwordActual = passwordActual;
      if (passwordNuevo) body.passwordNuevo = passwordNuevo;

      const res = await fetch(
        `${import.meta.env.VITE_API_BASE_URL ?? "https://fraude-detection-backend.onrender.com"}/api/usuarios/${user.numDocumento}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        },
      );

      const data = await res.json();

      if (!res.ok || data.success === false) {
        throw new Error(data.mensaje || data.error || "Error al actualizar");
      }

      // Actualizar contexto y localStorage
      const nuevoNombre =
        `${body.nombre ?? nombre.trim()} ${body.apellido ?? apellido.trim()}`.trim();
      updateUser({
        nombreCompleto: nuevoNombre,
        username: body.email ?? email.trim(),
      });

      toast.success("Perfil actualizado correctamente");
      setPasswordActual("");
      setPasswordNuevo("");
      setGuardado(true);
      setTimeout(() => setGuardado(false), 3000);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 max-w-md mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col items-center gap-3 pt-4">
        <div className="w-20 h-20 rounded-full bg-gradient-to-br from-purple-500 to-blue-500 flex items-center justify-center shadow-lg">
          <UserCircle className="w-12 h-12 text-white" />
        </div>
        <div className="text-center">
          <h1 className="text-xl font-bold">{user.nombreCompleto}</h1>
          <p className="text-sm text-gray-500">{user.username}</p>
          <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded-full font-medium">
            {user.rol}
          </span>
        </div>
      </div>

      {/* Información de cuenta */}
      <div className="bg-gray-50 rounded-2xl p-4 space-y-2 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-500">Documento</span>
          <span className="font-mono font-medium">{user.numDocumento}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500">Nº de cuenta</span>
          <span className="font-mono font-medium">{user.numeroCuenta}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500">Saldo</span>
          <span className="font-bold text-green-600">
            ${user.saldo?.toLocaleString("es-CO") ?? "0"}
          </span>
        </div>
      </div>

      {/* Formulario de edición */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <h2 className="font-bold text-base border-b pb-2">
          Editar información
        </h2>

        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1">
            <Label>Nombre</Label>
            <Input
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              placeholder="Nombre"
            />
          </div>
          <div className="space-y-1">
            <Label>Apellido</Label>
            <Input
              value={apellido}
              onChange={(e) => setApellido(e.target.value)}
              placeholder="Apellido"
            />
          </div>
        </div>

        <div className="space-y-1">
          <Label>Correo electrónico</Label>
          <Input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="tu@correo.com"
          />
        </div>

        <div className="border-t pt-4 space-y-1">
          <h3 className="text-sm font-semibold text-gray-500">
            Cambiar contraseña (opcional)
          </h3>
        </div>

        <div className="space-y-1">
          <Label>Contraseña actual</Label>
          <div className="relative">
            <Input
              type={showActual ? "text" : "password"}
              value={passwordActual}
              onChange={(e) => setPasswordActual(e.target.value)}
              placeholder="••••••••"
            />
            <button
              type="button"
              onClick={() => setShowActual((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
            >
              {showActual ? (
                <EyeOff className="w-4 h-4" />
              ) : (
                <Eye className="w-4 h-4" />
              )}
            </button>
          </div>
        </div>

        <div className="space-y-1">
          <Label>Nueva contraseña</Label>
          <div className="relative">
            <Input
              type={showNuevo ? "text" : "password"}
              value={passwordNuevo}
              onChange={(e) => setPasswordNuevo(e.target.value)}
              placeholder="••••••••"
              disabled={!passwordActual}
            />
            <button
              type="button"
              onClick={() => setShowNuevo((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
              disabled={!passwordActual}
            >
              {showNuevo ? (
                <EyeOff className="w-4 h-4" />
              ) : (
                <Eye className="w-4 h-4" />
              )}
            </button>
          </div>
        </div>

        <Button type="submit" className="w-full" disabled={loading}>
          {loading ? (
            "Guardando..."
          ) : guardado ? (
            <span className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4" /> Guardado
            </span>
          ) : (
            <span className="flex items-center gap-2">
              <Save className="w-4 h-4" /> Guardar cambios
            </span>
          )}
        </Button>
      </form>
    </div>
  );
};

export default PerfilPage;
