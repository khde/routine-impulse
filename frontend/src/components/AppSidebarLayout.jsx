import { Link, NavLink } from "react-router-dom";

export default function AppSidebarLayout({ title, subtitle, children }) {
  return (
    <main className="app-layout">
      <aside className="sidebar">
        <div>
          <h1 className="sidebar-title">
            <Link to="/dashboard" className="sidebar-brand-link">Routine Impulse</Link>
          </h1>

          <nav className="sidebar-nav" aria-label="Main navigation">
            <NavLink to="/dashboard" className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}>Dashboard</NavLink>
            <NavLink to="/routines" className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}>Routines</NavLink>
            <NavLink to="/tasks" className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}>Tasks</NavLink>
            <NavLink to="/calendar" className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}>Calendar</NavLink>
          </nav>
        </div>

        <div className="sidebar-bottom-nav" aria-label="Account and settings navigation">
          <NavLink to="/account/account" className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}>Account</NavLink>
          <NavLink to="/settings" className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}>Settings</NavLink>
          <NavLink to="/account/logout" className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}>Logout</NavLink>
        </div>
      </aside>

      <section className="dashboard-content">
        <h2>{title}</h2>
        {subtitle && <p className="info-text app-subtitle">{subtitle}</p>}
        {children}
      </section>
    </main>
  );
}