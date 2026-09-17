import { Check, Link2, ShieldCheck, UserPlus } from "lucide-react";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Button, Card, Notice, PageHeader } from "../../shared/components/ui";
import { acceptOrganizationInvite } from "../../shared/lib/saas-api";

export function InvitePage() {
  const { token } = useParams();
  const { session } = useAuth();
  const navigate = useNavigate();
  const demoMode = import.meta.env.DEV && import.meta.env.VITE_DEMO_MODE === "true";
  const [accepted, setAccepted] = useState(false);
  const accept = useMutation({
    mutationFn: () => demoMode ? Promise.resolve(undefined) : acceptOrganizationInvite(token ?? ""),
    onSuccess: () => setAccepted(true),
  });

  if (!session) return <main className="grid min-h-screen place-items-center bg-cream px-5 dark:bg-slate-950"><Card className="w-full max-w-md p-7 text-center"><span className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-sage-100 text-sage-700 dark:bg-sage-900 dark:text-sage-200"><UserPlus /></span><h1 className="mt-5 font-display text-2xl font-extrabold text-ink dark:text-white">Você recebeu um convite</h1><p className="mt-2 text-sm text-slate-500">Entre na sua conta para confirmar o convite com segurança.</p><Link to={`/login?invite=${encodeURIComponent(token ?? "")}`} className="mt-6 inline-flex min-h-10 items-center justify-center rounded-xl bg-sage-600 px-4 text-sm font-semibold text-white">Entrar para aceitar</Link></Card></main>;

  return <main className="grid min-h-screen place-items-center bg-cream px-5 dark:bg-slate-950"><Card className="w-full max-w-xl p-7"><PageHeader eyebrow="Convite de organização" title="Convite de acesso" description={`Olá, ${session.user.name}. Revise o acesso antes de aceitar este convite.`} /><div className="rounded-2xl border border-slate-200 p-5 dark:border-slate-700"><div className="flex items-start gap-3"><ShieldCheck className="mt-0.5 text-sage-600" size={20} /><div><p className="font-bold text-ink dark:text-white">Acesso definido pelo administrador</p><p className="mt-1 text-sm text-slate-500">Sua permissão será vinculada à organização do convite. Dados clínicos continuam sujeitos à autorização contextual.</p></div></div><div className="mt-4 flex items-center gap-2 text-xs text-slate-400"><Link2 size={14} /> Convite temporário · token protegido</div></div>{demoMode && <div className="mt-5"><Notice title="Ambiente de demonstração">A aceitação é local apenas neste cenário. No ambiente real, o token será validado no backend e a membership será criada atomicamente.</Notice></div>}{accept.isError && <div className="mt-5"><Notice title="Não foi possível aceitar" tone="warning">{accept.error instanceof Error ? accept.error.message : "O convite é inválido, expirou ou pertence a outro e-mail."}</Notice></div>}{accepted ? <div className="mt-6"><Notice title="Convite aceito" tone="success">Sua membership foi criada. O token não poderá ser reutilizado.</Notice><Button className="mt-4" onClick={() => navigate("/dashboard")}>Abrir PsicoGest</Button></div> : <Button className="mt-6 w-full" disabled={!token || accept.isPending} onClick={() => accept.mutate()}><Check size={16} /> {accept.isPending ? "Validando convite…" : "Aceitar convite"}</Button>}</Card></main>;
}
