import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { AppProviders } from "./app/providers";
import { AppRouter } from "./app/router";
import "./index.css";

createRoot(document.getElementById("root")!).render(<StrictMode><BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><AppProviders><AppRouter /></AppProviders></BrowserRouter></StrictMode>);
