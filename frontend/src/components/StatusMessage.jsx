export default function StatusMessage({ message, ok = false }) {
  return <p className={ok ? "status ok" : "status"}>{message}</p>;
}