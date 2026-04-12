import { Link } from "react-router-dom";
import PageShell from "../components/PageShell";

export default function StartPage({ loggedIn }) {
  return (
    <PageShell
      title="Routine Impulse"
      subtitle="A simple routine tracker for daily focus and consistency."
      wide
    >

      <div className="action-row">
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
              Signup
            </Link>
          </>
        )}
      </div>
    </PageShell>
  );
}