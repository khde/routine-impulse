import { Link } from "react-router-dom";
import PageShell from "../components/PageShell";

export default function StartPage({ loggedIn }) {
  return (
    <PageShell
      title="Routine Impulse"
      subtitle="Small routines. Steady progress."
      wide
      className="start-page"
    >
      <div className={loggedIn ? "action-row single-action" : "action-row"}>
        {loggedIn ? (
          <Link className="link-button primary-link" to="/dashboard">
            Go to Dashboard
          </Link>
        ) : (
          <>
            <Link className="link-button primary-link" to="/login">
              Login
            </Link>
            <Link className="link-button secondary-link" to="/signup">
              Create account
            </Link>
          </>
        )}
      </div>
    </PageShell>
  );
}