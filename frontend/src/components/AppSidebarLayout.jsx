import { useEffect, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import {
  Activity,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  CircleUserRound,
  LayoutDashboard,
  ListTodo,
  LogOut,
  Repeat2,
  Settings2
} from "lucide-react";

const MAIN_NAV_ITEMS = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/routines", label: "Routines", icon: Repeat2 },
  { to: "/tasks", label: "Tasks", icon: ListTodo },
  { to: "/calendar", label: "Calendar", icon: CalendarDays }
];

const ACCOUNT_NAV_ITEMS = [
  { to: "/account/account", label: "Account", icon: CircleUserRound },
  { to: "/settings", label: "Settings", icon: Settings2 },
  { to: "/account/logout", label: "Logout", icon: LogOut }
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
              <Link to="/dashboard" className="sidebar-brand-link">
                <span className="sidebar-brand-mark" aria-hidden="true"><Activity size={19} strokeWidth={2.2} /></span>
                {!collapsed && <span className="sidebar-brand-name">Routine Impulse</span>}
                {collapsed && <span className="sidebar-brand-initials">RI</span>}
              </Link>
            </h1>

            <button
              type="button"
              className="sidebar-collapse-toggle"
              aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
              aria-pressed={collapsed}
              onClick={toggleCollapsed}
            >
              {collapsed ? <ChevronRight size={17} /> : <ChevronLeft size={17} />}
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
                <span className="sidebar-item-icon" aria-hidden="true"><item.icon size={17} strokeWidth={1.8} /></span>
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
              <span className="sidebar-item-icon" aria-hidden="true"><item.icon size={17} strokeWidth={1.8} /></span>
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