import type { DataSource } from "./mock-api";
import { mockApi } from "@psicogest/demo-api";
import { realApi } from "./real-api";

/**
 * Adapter único: demo é opt-in explícito; produção sempre fala com o backend.
 *
 * O módulo usado aqui é trocado pelo Vite: desenvolvimento pode usar dados de
 * demonstração; a build de produção recebe apenas um adapter que falha fechado.
 */
const isDemoMode = import.meta.env.DEV && import.meta.env.VITE_DEMO_MODE === "true";

export const dataSource: DataSource = isDemoMode ? mockApi : realApi;
