import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { AuthProvider } from "../features/auth/AuthContext";
import { ThemeProvider } from "../shared/providers/ThemeProvider";
import { ErrorBoundary } from "../shared/components/ErrorBoundary";
import { ToastProvider } from "../shared/providers/ToastProvider";

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false } } }));
  return <ErrorBoundary><ThemeProvider><ToastProvider><QueryClientProvider client={queryClient}><AuthProvider>{children}</AuthProvider></QueryClientProvider></ToastProvider></ThemeProvider></ErrorBoundary>;
}
