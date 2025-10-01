import { useEffect, useState } from "react";

export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<"checking" | "authed" | "guest">("checking");

  useEffect(() => {
    (async () => {
      const base = import.meta.env.VITE_API_BASE ?? "/api";
      const res = await fetch(`${base}/auth/oauth2/check`, { credentials: "include" });
      setState(res.ok ? "authed" : "guest");
    })();
  }, []);

  if (state === "checking") return null; // or a spinner
  if (state === "guest") { window.location.replace("/login"); return null; }
  return <>{children}</>;
}