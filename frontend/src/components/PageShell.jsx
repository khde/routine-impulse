export default function PageShell({ title, subtitle, wide = false, className = "", children }) {
  return (
    <main className={`page ${className}`.trim()}>
      <section className={`${wide ? "card wide" : "card"} ${className ? `${className}-card` : ""}`.trim()}>
        <h1>{title}</h1>
        {subtitle && <p className="subtitle">{subtitle}</p>}
        {children}
      </section>
    </main>
  );
}