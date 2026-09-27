import { useEffect, useMemo, useRef, useState } from "react";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { ArrowLeft, CheckCircle2, KeyRound, MailCheck } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Button, Card, Input } from "../../shared/components/ui";
import { apiClient } from "../../shared/lib/api-client";

const emailSchema = z.object({ email: z.string().trim().email("Informe um e-mail válido.").max(150) });
const passwordSchema = z.object({
  password: z.string().min(12, "Use pelo menos 12 caracteres.").max(72, "A senha excede o limite permitido."),
  confirmation: z.string(),
}).refine((value) => value.password === value.confirmation, {
  path: ["confirmation"],
  message: "As senhas não coincidem.",
});

type EmailValues = z.infer<typeof emailSchema>;
type PasswordValues = z.infer<typeof passwordSchema>;
type AuthActionResponse = { message: string };

export function AuthActionPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const verification = location.pathname === "/verify-email";
  const token = useMemo(() => new URLSearchParams(location.hash.slice(1)).get("token") ?? "", [location.hash]);
  const handledVerification = useRef("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!token) return;
    // Tokens arrive in the fragment so they are not sent in HTTP requests; clear browser history immediately.
    window.history.replaceState(null, "", `${location.pathname}${location.search}`);
  }, [location.pathname, location.search, token]);

  useEffect(() => {
    if (!verification || !token) return;
    const actionKey = `${location.pathname}:${token}`;
    if (handledVerification.current === actionKey) return;
    handledVerification.current = actionKey;
    setBusy(true);
    void apiClient.request<void>("/auth/email/verify", {
      method: "POST",
      body: JSON.stringify({ token }),
    }).then(() => setNotice("E-mail confirmado. Você já pode entrar na sua conta."))
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : "Não foi possível confirmar o e-mail."))
      .finally(() => setBusy(false));
  }, [location.pathname, token, verification]);

  const hasToken = Boolean(token);
  const title = verification ? (hasToken ? "Confirme seu e-mail" : "Reenviar confirmação")
    : (hasToken ? "Crie uma nova senha" : "Recuperar acesso");
  const description = verification
    ? (hasToken ? "Estamos validando seu link seguro." : "Se o endereço estiver cadastrado, enviaremos um link de uso único.")
    : (hasToken ? "Escolha uma senha forte para proteger sua conta." : "Informe seu e-mail para receber um link de recuperação, se a conta existir.");

  async function requestAction(values: EmailValues) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const path = verification ? "/auth/email/verification-request" : "/auth/password/reset-request";
      const response = await apiClient.request<AuthActionResponse>(path, {
        method: "POST",
        body: JSON.stringify({ email: values.email }),
      });
      setNotice(response.message);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Não foi possível processar a solicitação.");
    } finally {
      setBusy(false);
    }
  }

  async function completePassword(values: PasswordValues) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await apiClient.request<void>("/auth/password/reset", {
        method: "POST",
        body: JSON.stringify({ token, newPassword: values.password }),
      });
      setNotice("Senha atualizada. Todas as sessões anteriores foram encerradas; entre novamente.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Não foi possível redefinir a senha.");
    } finally {
      setBusy(false);
    }
  }

  return <main className="flex min-h-screen items-center justify-center bg-cream px-4 py-10 dark:bg-slate-950">
    <Card className="w-full max-w-md border-0 p-7 shadow-xl sm:p-9 dark:border dark:border-slate-800">
      <Link to="/login" className="mb-8 inline-flex items-center gap-2 text-sm font-semibold text-sage-700 hover:underline dark:text-sage-300">
        <ArrowLeft size={16} /> Voltar ao acesso
      </Link>
      <div className="mb-6 grid h-12 w-12 place-items-center rounded-2xl bg-sage-50 text-sage-700 dark:bg-sage-900/40 dark:text-sage-300">
        {verification ? <MailCheck size={23} /> : <KeyRound size={23} />}
      </div>
      <p className="text-xs font-bold uppercase tracking-[0.18em] text-sage-600">PsicoGest · Segurança da conta</p>
      <h1 className="mt-2 font-display text-3xl font-extrabold tracking-tight text-ink dark:text-white">{title}</h1>
      <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{description}</p>

      {busy && verification && hasToken && <p className="mt-6 text-sm text-slate-600" role="status">Validando o link…</p>}
      {!hasToken && !notice && <EmailRequestForm verification={verification} busy={busy} onSubmit={requestAction} />}
      {!verification && hasToken && !notice && <PasswordResetForm busy={busy} onSubmit={completePassword} />}
      {notice && <div className="mt-6 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm leading-6 text-emerald-900 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-200" role="status"><CheckCircle2 className="mr-2 inline" size={17} />{notice}</div>}
      {error && <p className="mt-5 rounded-xl bg-red-50 p-3 text-sm font-medium text-red-700 dark:bg-red-950/40 dark:text-red-300" role="alert">{error}</p>}
      {error && hasToken && <Link className="mt-4 inline-block text-sm font-semibold text-sage-700 hover:underline dark:text-sage-300" to={verification ? "/verify-email" : "/reset-password"}>Solicitar um novo link</Link>}
      {notice && <Button className="mt-6 w-full" onClick={() => navigate("/login", { replace: true })}>Ir para o login</Button>}
    </Card>
  </main>;
}

function EmailRequestForm({ verification, busy, onSubmit }: {
  verification: boolean;
  busy: boolean;
  onSubmit: (values: EmailValues) => Promise<void>;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<EmailValues>({ resolver: zodResolver(emailSchema) });
  return <form className="mt-6 space-y-5" onSubmit={handleSubmit(onSubmit)} noValidate>
    <Input label="E-mail da conta" type="email" autoComplete="email" error={errors.email?.message} {...register("email")} />
    <Button className="w-full" type="submit" disabled={busy}>{busy ? "Enviando…" : verification ? "Enviar link de confirmação" : "Enviar link de recuperação"}</Button>
  </form>;
}

function PasswordResetForm({ busy, onSubmit }: {
  busy: boolean;
  onSubmit: (values: PasswordValues) => Promise<void>;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<PasswordValues>({ resolver: zodResolver(passwordSchema) });
  return <form className="mt-6 space-y-5" onSubmit={handleSubmit(onSubmit)} noValidate>
    <Input label="Nova senha" type="password" autoComplete="new-password" hint="Mínimo de 12 caracteres." error={errors.password?.message} {...register("password")} />
    <Input label="Confirme a nova senha" type="password" autoComplete="new-password" error={errors.confirmation?.message} {...register("confirmation")} />
    <Button className="w-full" type="submit" disabled={busy}>{busy ? "Atualizando…" : "Salvar nova senha"}</Button>
  </form>;
}
