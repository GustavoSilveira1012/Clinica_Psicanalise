import { useEffect, useMemo, useRef, useState, type KeyboardEvent as ReactKeyboardEvent } from "react";
import { ArrowRight, Command, CornerDownLeft, Search } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { cn } from "../lib/cn";
import { Button, IconButton, Modal } from "./ui";

export interface CommandPaletteItem {
  label: string;
  description: string;
  group: string;
  to: string;
  keywords?: string[];
}

interface CommandPaletteProps {
  items: CommandPaletteItem[];
}

export function CommandPalette({ items }: CommandPaletteProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const inputRef = useRef<HTMLInputElement>(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [selectedIndex, setSelectedIndex] = useState(0);

  const results = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("pt-BR");
    if (!normalizedQuery) return items;
    return items.filter((item) => [item.label, item.description, item.group, ...(item.keywords ?? [])].join(" ").toLocaleLowerCase("pt-BR").includes(normalizedQuery));
  }, [items, query]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setOpen(true);
      }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, []);

  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [query, open]);

  const openPalette = () => {
    setQuery("");
    setOpen(true);
  };

  const selectItem = (item: CommandPaletteItem) => {
    navigate(item.to);
    setOpen(false);
  };

  const handleInputKeyDown = (event: ReactKeyboardEvent<HTMLInputElement>) => {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setSelectedIndex((current) => Math.min(current + 1, Math.max(results.length - 1, 0)));
    }
    if (event.key === "ArrowUp") {
      event.preventDefault();
      setSelectedIndex((current) => Math.max(current - 1, 0));
    }
    if (event.key === "Enter" && results[selectedIndex]) {
      event.preventDefault();
      selectItem(results[selectedIndex]);
    }
  };

  return (
    <>
      <Button type="button" variant="secondary" className="hidden h-9 min-h-9 border-slate-200 px-3 text-xs dark:border-slate-700 sm:inline-flex" onClick={openPalette}>
        <Search size={15} />
        Buscar
        <kbd className="ml-1 hidden rounded-md border border-slate-200 bg-slate-50 px-1.5 py-0.5 font-mono text-[10px] text-slate-400 lg:inline dark:border-slate-700 dark:bg-slate-800">Ctrl K</kbd>
      </Button>
      <IconButton label="Busca rápida" className="sm:hidden" onClick={openPalette}>
        <Search size={18} />
      </IconButton>

      <Modal open={open} title="Busca rápida" description="Encontre uma área do PsicoGest sem interromper seu fluxo." onClose={() => setOpen(false)} initialFocusRef={inputRef}>
        <div className="-mx-1">
          <label className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 focus-within:border-sage-500 focus-within:ring-2 focus-within:ring-sage-500/20 dark:border-slate-700" htmlFor="command-palette-search">
            <Search className="shrink-0 text-slate-400" size={18} />
            <input ref={inputRef} id="command-palette-search" value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={handleInputKeyDown} placeholder="Buscar agenda, pacientes, financeiro…" className="min-h-12 min-w-0 flex-1 bg-transparent text-sm text-ink outline-none placeholder:text-slate-400 dark:text-white" aria-controls="command-palette-results" aria-activedescendant={results[selectedIndex] ? `command-option-${results[selectedIndex].to}` : undefined} autoComplete="off" />
            <kbd className="hidden items-center gap-1 rounded-md bg-slate-100 px-1.5 py-1 font-mono text-[10px] text-slate-500 sm:inline-flex dark:bg-slate-800 dark:text-slate-400"><Command size={11} /> K</kbd>
          </label>
          <div id="command-palette-results" className="mt-3 max-h-[min(52vh,360px)] overflow-y-auto" role="listbox" aria-label="Áreas disponíveis">
            {results.length === 0 ? <div className="rounded-xl bg-slate-50 p-6 text-center dark:bg-slate-800/60"><Search className="mx-auto text-slate-400" size={20} /><p className="mt-2 text-sm font-semibold text-ink dark:text-white">Nenhum resultado</p><p className="mt-1 text-xs text-slate-500">Tente outro termo ou use o menu lateral.</p></div> : <div className="space-y-1">{results.map((item, index) => <button key={item.to} id={`command-option-${item.to}`} type="button" role="option" aria-selected={selectedIndex === index} onMouseEnter={() => setSelectedIndex(index)} onClick={() => selectItem(item)} className={cn("flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left transition", selectedIndex === index ? "bg-sage-50 dark:bg-sage-950/40" : "hover:bg-slate-50 dark:hover:bg-slate-800/70")}><span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-white text-sage-700 shadow-sm dark:bg-slate-900 dark:text-sage-300"><ArrowRight size={16} /></span><span className="min-w-0 flex-1"><span className="block text-sm font-bold text-ink dark:text-white">{item.label}</span><span className="mt-0.5 block truncate text-xs text-slate-500">{item.group} · {item.description}</span></span>{selectedIndex === index && <CornerDownLeft className="shrink-0 text-slate-400" size={15} />}</button>)}</div>}
          </div>
          <p className="mt-4 flex items-center gap-2 text-[11px] text-slate-400"><CornerDownLeft size={13} /> Use as setas para navegar e Enter para abrir</p>
        </div>
      </Modal>
    </>
  );
}
