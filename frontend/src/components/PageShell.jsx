export default function PageShell({ title, subtitle, wide = false, children }) {
  return (
    <main className="page">
      <section className={wide ? "card wide" : "card"}>
        <h1>{title}</h1>
        {subtitle && <p className="subtitle">{subtitle}</p>}
        {children}
      </section>
    </main>
  );
}