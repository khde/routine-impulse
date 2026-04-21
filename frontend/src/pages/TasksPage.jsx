import { useEffect, useMemo, useState } from "react";
import AppSidebarLayout from "../components/AppSidebarLayout";
import { parseError } from "../utils/parseError";

const TASK_API_BASE = "/api/task";

export default function TasksPage({ apiFetch }) {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newDescription, setNewDescription] = useState("");
  const [newDueDate, setNewDueDate] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [busyTaskId, setBusyTaskId] = useState(null);

  useEffect(() => {
    loadTasks();
  }, []);

  const openTasks = useMemo(
    () => tasks.filter((task) => !task.completed).sort((a, b) => sortByDueDate(a, b)),
    [tasks]
  );

  const completedTasks = useMemo(
    () => tasks.filter((task) => task.completed).sort((a, b) => sortByDueDate(a, b)),
    [tasks]
  );

  async function loadTasks() {
    setLoading(true);
    setStatus("");

    try {
      const response = await apiFetch(`${TASK_API_BASE}/all`, { method: "GET" });
      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const data = await response.json();
      setTasks(Array.isArray(data) ? data : []);
    } catch (error) {
      setStatus(error.message || "Failed to load tasks.");
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateTask(event) {
    event.preventDefault();
    const description = newDescription.trim();

    if (!description) {
      setStatus("Task description is required.");
      return;
    }

    setSubmitting(true);
    setStatus("");

    try {
      const response = await apiFetch(`${TASK_API_BASE}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          description,
          completed: false,
          dueDate: newDueDate ? newDueDate : null
        })
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const createdTask = await response.json();
      setTasks((prev) => [createdTask, ...prev]);
      setNewDescription("");
      setNewDueDate("");
      setShowCreateModal(false);
      setStatus("Task created.");
    } catch (error) {
      setStatus(error.message || "Failed to create task.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSetCompleted(task, completed) {
    setBusyTaskId(task.id);
    setStatus("");

    try {
      const response = await apiFetch(`${TASK_API_BASE}/${task.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ completed })
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const updatedTask = await response.json();
      setTasks((prev) => prev.map((item) => (item.id === updatedTask.id ? updatedTask : item)));
    } catch (error) {
      setStatus(error.message || "Failed to update task.");
    } finally {
      setBusyTaskId(null);
    }
  }

  async function handleDeleteTask(taskId) {
    setBusyTaskId(taskId);
    setStatus("");

    try {
      const response = await apiFetch(`${TASK_API_BASE}/${taskId}`, {
        method: "DELETE"
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      setTasks((prev) => prev.filter((task) => task.id !== taskId));
      setStatus("Task deleted.");
    } catch (error) {
      setStatus(error.message || "Failed to delete task.");
    } finally {
      setBusyTaskId(null);
    }
  }

  return (
    <AppSidebarLayout
      title="Tasks"
    >
      <div className="tasks-toolbar">
        <button
          type="button"
          className="tasks-new-button"
          onClick={() => {
            setShowCreateModal(true);
            setStatus("");
          }}
        >
          New Task
        </button>
      </div>

      {status && <p className="status">{status}</p>}

      <section className="task-section">
        <h3 className="task-section-title">Open Tasks</h3>
        {loading ? (
          <p className="info-text">Loading tasks...</p>
        ) : openTasks.length === 0 ? (
          <p className="info-text">No open tasks.</p>
        ) : (
          <div className="task-table-wrap">
            <table className="task-table">
              <thead>
                <tr>
                  <th>Description</th>
                  <th>Due Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {openTasks.map((task) => (
                  <tr key={task.id}>
                    <td>{task.description}</td>
                    <td>{formatDate(task.dueDate)}</td>
                    <td className="task-actions-cell">
                      <button
                        type="button"
                        className="table-action"
                        disabled={busyTaskId === task.id}
                        onClick={() => handleSetCompleted(task, true)}
                      >
                        Complete
                      </button>
                      <button
                        type="button"
                        className="table-action danger"
                        disabled={busyTaskId === task.id}
                        onClick={() => handleDeleteTask(task.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="task-section completed">
        <h3 className="task-section-title">Completed Tasks</h3>
        {completedTasks.length === 0 ? (
          <p className="info-text">No completed tasks yet.</p>
        ) : (
          <div className="task-table-wrap">
            <table className="task-table">
              <thead>
                <tr>
                  <th>Description</th>
                  <th>Due Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {completedTasks.map((task) => (
                  <tr key={task.id} className="task-completed-row">
                    <td>{task.description}</td>
                    <td>{formatDate(task.dueDate)}</td>
                    <td className="task-actions-cell">
                      <button
                        type="button"
                        className="table-action"
                        disabled={busyTaskId === task.id}
                        onClick={() => handleSetCompleted(task, false)}
                      >
                        Reopen
                      </button>
                      <button
                        type="button"
                        className="table-action danger"
                        disabled={busyTaskId === task.id}
                        onClick={() => handleDeleteTask(task.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {showCreateModal && (
        <div className="modal-overlay" role="presentation" onClick={() => !submitting && setShowCreateModal(false)}>
          <section
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-label="Create task"
            onClick={(event) => event.stopPropagation()}
          >
            <h3 className="modal-title">Create New Task</h3>
            <form className="task-create-form" onSubmit={handleCreateTask}>
              <label>
                Task
                <input
                  type="text"
                  value={newDescription}
                  onChange={(event) => setNewDescription(event.target.value)}
                  maxLength={255}
                  placeholder="What do you want to do?"
                  autoFocus
                  disabled={submitting}
                />
              </label>
              <label>
                Due Date (optional)
                <input
                  type="datetime-local"
                  value={newDueDate}
                  onChange={(event) => setNewDueDate(event.target.value)}
                  disabled={submitting}
                />
              </label>
              <div className="modal-actions">
                <button type="submit" className="primary" disabled={submitting}>
                  {submitting ? "Saving..." : "Save Task"}
                </button>
                <button
                  type="button"
                  className="secondary"
                  disabled={submitting}
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </AppSidebarLayout>
  );
}

function formatDate(value) {
  if (!value) {
    return "-";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

function sortByDueDate(left, right) {
  const leftTime = left.dueDate ? new Date(left.dueDate).getTime() : Number.POSITIVE_INFINITY;
  const rightTime = right.dueDate ? new Date(right.dueDate).getTime() : Number.POSITIVE_INFINITY;

  if (leftTime === rightTime) {
    return (left.id || 0) - (right.id || 0);
  }

  return leftTime - rightTime;
}