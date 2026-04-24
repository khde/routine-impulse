import { useEffect, useState } from "react";
import { Link, NavLink } from "react-router-dom";

const MAIN_NAV_ITEMS = [
  { to: "/dashboard", label: "Dashboard", icon: "⌂" },
  { to: "/routines", label: "Routines", icon: "⟳" },
  { to: "/tasks", label: "Tasks", icon: "✓" },
  { to: "/calendar", label: "Calendar", icon: "◷" }
];

const ACCOUNT_NAV_ITEMS = [
  { to: "/account/account", label: "Account", icon: "◉" },
  { to: "/settings", label: "Settings", icon: "⚙" },
  { to: "/account/logout", label: "Logout", icon: "⇥" }
];

export default function AppSidebarLayout({ title, subtitle, children }) {
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    const value = window.localStorage.getItem("sidebar-collapsed");
    if (value === "1") {
      setCollapsed(true);
    }
  }, []);

  function toggleCollapsed() {
    setCollapsed((prev) => {
      const next = !prev;
      window.localStorage.setItem("sidebar-collapsed", next ? "1" : "0");
      return next;
    });
  }

  return (
    <main className={collapsed ? "app-layout sidebar-collapsed" : "app-layout"}>
      <aside className={collapsed ? "sidebar collapsed" : "sidebar"}>
        <div>
          <div className="sidebar-header">
            <h1 className="sidebar-title">
              <Link to="/dashboard" className="sidebar-brand-link">{collapsed ? "RI" : "Routine Impulse"}</Link>
            </h1>

            <button
              type="button"
              className="sidebar-collapse-toggle"
              aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
              aria-pressed={collapsed}
              onClick={toggleCollapsed}
            >
              {collapsed ? "»" : "«"}
            </button>
          </div>

          <nav className="sidebar-nav" aria-label="Main navigation">
            {MAIN_NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                title={collapsed ? item.label : undefined}
                className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}
              >
                <span className="sidebar-item-icon" aria-hidden="true">{item.icon}</span>
                {!collapsed && <span className="sidebar-item-label">{item.label}</span>}
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="sidebar-bottom-nav" aria-label="Account and settings navigation">
          {ACCOUNT_NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              title={collapsed ? item.label : undefined}
              className={({ isActive }) => isActive ? "sidebar-item active" : "sidebar-item"}
            >
              <span className="sidebar-item-icon" aria-hidden="true">{item.icon}</span>
              {!collapsed && <span className="sidebar-item-label">{item.label}</span>}
            </NavLink>
          ))}
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