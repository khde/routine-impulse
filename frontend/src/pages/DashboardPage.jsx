export default function DashboardPage() {
  return (
    <main className="app-layout">
      <aside className="sidebar">
        <div>
          <h1 className="sidebar-title">Routine Impulse</h1>

          <nav className="sidebar-nav" aria-label="Main navigation">
            <button type="button" className="sidebar-item active">Dashboard</button>
            <button type="button" className="sidebar-item">Routines</button>
            <button type="button" className="sidebar-item">Tasks</button>
            <button type="button" className="sidebar-item">Calender</button>
          </nav>
        </div>

        <div>
          <section className="sidebar-account" aria-label="Account menu">
            <p className="sidebar-section-title">Account</p>
            <button type="button" className="sidebar-sub-item">Profile</button>
            <button type="button" className="sidebar-sub-item">Logout</button>
          </section>

          <button type="button" className="sidebar-item settings">Settings</button>
        </div>
      </aside>

      <section className="dashboard-content">
        <h2>Dashboard</h2>
        <p className="info-text">Main content area.</p>
      </section>
    </main>
  );
}