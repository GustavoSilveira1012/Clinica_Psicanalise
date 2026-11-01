import { Link } from "react-router-dom";
import { PublicFunnelLayout } from "./PublicFunnelLayout";

const questions = [
  { question: "Posso conhecer o produto sem conversar com alguém?", answer: "Sim. A demonstração autoguiada apresenta os fluxos principais com exemplos fictícios e não pede cadastro." },
  { question: "Já posso criar uma clínica e cadastrar pacientes reais?", answer: "Ainda não. A abertura para novos clientes depende da conclusão das validações operacionais e de segurança do piloto." },
  { question: "O teste gratuito e a contratação já estão disponíveis?", answer: "Não. A página de planos mostrará preços somente após aprovação. O teste público e o checkout permanecem fechados." },
  { question: "A demonstração usa informações de pacientes?", answer: "Não. Os exemplos mostrados são fictícios e a prévia não salva informações clínicas." },
  { question: "Tenho uma conta de piloto. Como acesso?", answer: "Use o acesso nominal fornecido pela equipe responsável e entre pela página de login. Não utilize uma conta compartilhada." },
];

export function PublicFaqPage() {
  return <PublicFunnelLayout>
    <section className="mx-auto max-w-4xl px-5 pt-10 sm:px-8 lg:pt-20">
      <p className="text-sm font-semibold text-harbor-700 dark:text-harbor-300">Dúvidas frequentes</p>
      <h1 className="mt-3 font-display text-4xl font-extrabold tracking-tight sm:text-5xl">Respostas para começar.</h1>
      <div className="mt-9 space-y-3">{questions.map(({ question, answer }) => <details key={question} className="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900"><summary className="cursor-pointer font-semibold">{question}</summary><p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">{answer}</p></details>)}</div>
      <Link to="/demo" className="mt-8 inline-flex min-h-11 items-center rounded-xl bg-harbor-600 px-5 text-sm font-semibold text-white hover:bg-harbor-700">Explorar demonstração</Link>
    </section>
  </PublicFunnelLayout>;
}
