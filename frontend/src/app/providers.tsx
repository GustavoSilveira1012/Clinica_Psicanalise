import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { AuthProvider, useAuth } from "../features/auth/AuthContext";
import { ThemeProvider } from "../shared/providers/ThemeProvider";
import { ErrorBoundary } from "../shared/components/ErrorBoundary";
import { ToastProvider } from "../shared/providers/ToastProvider";

export function AppProviders({ children }: { children: ReactNode }) {
  return <ErrorBoundary><ThemeProvider><ToastProvider><AuthProvider><ScopedQueryProvider>{children}</ScopedQueryProvider></AuthProvider></ToastProvider></ThemeProvider></ErrorBoundary>;
}

function ScopedQueryProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  const cacheScope = `${session?.user.id ?? "anonymous"}:${session?.user.tenant.id ?? "none"}`;
  return <TenantQueryProvider key={cacheScope}>{children}</TenantQueryProvider>;
}

function TenantQueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false } },
  }));

  useEffect(() => () => queryClient.clear(), [queryClient]);

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
