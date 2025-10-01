import { useEffect } from "react";

export default function OAuthSuccess() {
  useEffect(() => {
    const hash = window.location.hash; // e.g., #t=eyJhbGciOi...
    const params = new URLSearchParams(hash.startsWith("#") ? hash.slice(1) : hash);
    const token = params.get("t");

    console.log('🔐 OAuthSuccess: hash =', hash);
    console.log('🔐 OAuthSuccess: token =', token ? token.substring(0, 20) + '...' : 'null');

    const finish = async () => {
      try {
        if (token) {
          console.log('🔐 OAuthSuccess: Found token, exchanging for session...');
          
          // Immediately remove the fragment from the address bar
          history.replaceState(null, "", "/oauth-success");

          // Exchange token => backend sets HttpOnly cookie in a normal 200
          const res = await fetch(`${import.meta.env.VITE_API_BASE ?? "/api"}/auth/session`, {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ token }),
          });
          
          console.log('🔐 OAuthSuccess: Session exchange response:', res.status);
          if (!res.ok) throw new Error(`session exchange failed: ${res.status}`);

          // Double-check session
          const check = await fetch(`${import.meta.env.VITE_API_BASE ?? "/api"}/auth/oauth2/check`, {
            method: "GET",
            credentials: "include",
          });
          
          console.log('🔐 OAuthSuccess: Session check response:', check.status);
          if (!check.ok) throw new Error(`check failed: ${check.status}`);

          console.log('🔐 OAuthSuccess: Success! Redirecting to root...');
          // Redirect to root and let AuthProvider handle routing
          window.location.replace("/");
          return;
        }

        // No fragment token? Try the normal check (in case cookie was set by 302)
        const check = await fetch(`${import.meta.env.VITE_API_BASE ?? "/api"}/auth/oauth2/check`, {
          method: "GET",
          credentials: "include",
        });
        if (check.ok) {
          window.location.replace("/");
        } else {
          // show a friendly message or route to login
          window.location.replace("/login");
        }
      } catch (e) {
        console.error(e);
        window.location.replace("/login");
      }
    };

    void finish();
  }, []);

  return <div style={{ padding: 24 }}>Signing you in…</div>;
}