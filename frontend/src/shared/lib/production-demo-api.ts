import type { DataSource } from "./mock-api";

/** Adapter usado na build de produção para garantir que demo não seja executado. */
export const mockApi: DataSource = new Proxy({} as DataSource, {
  get: () => () => {
    throw new Error("Modo de demonstração não está disponível nesta build.");
  },
});
