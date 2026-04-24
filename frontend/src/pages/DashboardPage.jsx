import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppSidebarLayout from "../components/AppSidebarLayout";
import { USER_ACCOUNT_API } from "../config/api";
import { parseError } from "../utils/parseError";

const TASK_API_BASE = "/api/task";
const ROUTINE_API_BASE = "/api/routine";

export default function DashboardPage({ apiFetch }) {
  const navigate = useNavigate();
  const [account, setAccount] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [routines, setRoutines] = useState([]);
  const [todayRoutineItems, setTodayRoutineItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [busyTaskId, setBusyTaskId] = useState(null);
  const [busyRoutineId, setBusyRoutineId] = useState(null);

  const openTasks = useMemo(
    () => tasks.filter((task) => !task.completed),
    [tasks]
  );

  const overdueTasks = useMemo(() => {
    const now = new Date();
    return openTasks
      .filter((task) => task.dueDate && new Date(task.dueDate) < now)
      .sort((left, right) => new Date(left.dueDate).getTime() - new Date(right.dueDate).getTime());
  }, [openTasks]);

  const upcomingTasks = useMemo(() => {
    const now = new Date();

    return openTasks
      .filter((task) => task.dueDate && new Date(task.dueDate) >= now)
      .sort(sortByDueDate)
      .slice(0, 5);
  }, [openTasks]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  async function loadDashboardData() {
    setLoading(true);
    setStatus("");

    try {
      const [accountResponse, tasksResponse, routinesResponse] = await Promise.all([
        apiFetch(USER_ACCOUNT_API, { method: "GET" }),
        apiFetch(`${TASK_API_BASE}/all`, { method: "GET" }),
        apiFetch(`${ROUTINE_API_BASE}/all`, { method: "GET" })
      ]);

      if (!accountResponse.ok) {
        throw new Error(await parseError(accountResponse));
      }

      if (!tasksResponse.ok) {
        throw new Error(await parseError(tasksResponse));
      }

      if (!routinesResponse.ok) {
        throw new Error(await parseError(routinesResponse));
      }

      const accountData = await accountResponse.json();
      const taskData = await tasksResponse.json();
      const routineData = await routinesResponse.json();

      setAccount(accountData || null);
      setTasks(Array.isArray(taskData) ? taskData : []);
      setRoutines(Array.isArray(routineData) ? routineData : []);

      await loadTodayRoutines(Array.isArray(routineData) ? routineData : []);
    } catch (error) {
      setStatus(error.message || "Failed to load dashboard.");
    } finally {
      setLoading(false);
    }
  }

  async function loadTodayRoutines(routineList) {
    const today = getTodayDateString();

    if (routineList.length === 0) {
      setTodayRoutineItems([]);
      return;
    }

    try {
      const query = new URLSearchParams({
        from: today,
        to: today,
        status: "ALL"
      });

      const responses = await Promise.all(
        routineList.map((routine) =>
          apiFetch(`${ROUTINE_API_BASE}/${routine.id}/activity?${query.toString()}`, {
            method: "GET"
          })
        )
      );

      const loadedItems = [];

      for (let index = 0; index < responses.length; index += 1) {
        const response = responses[index];
        const routine = routineList[index];

        if (!response.ok) {
          continue;
        }

        const activityList = await response.json();
        const todayActivity = Array.isArray(activityList)
          ? activityList.find((entry) => entry.date === today)
          : null;

        if (todayActivity) {
          loadedItems.push({
            routineId: routine.id,
            routineName: routine.name,
            completed: !!todayActivity.completed
          });
        }
      }

      loadedItems.sort((left, right) => left.routineName.localeCompare(right.routineName));
      setTodayRoutineItems(loadedItems);
    } catch {
      setTodayRoutineItems([]);
    }
  }

  async function handleSetTaskCompleted(task, completed) {
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

  async function handleSetTodayRoutineCompleted(item, completed) {
    const today = getTodayDateString();

    setBusyRoutineId(item.routineId);
    setStatus("");

    try {
      const response = await apiFetch(`${ROUTINE_API_BASE}/${item.routineId}/activity`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          date: today,
          completed
        })
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const updatedActivity = await response.json();
      setTodayRoutineItems((prev) =>
        prev.map((entry) =>
          entry.routineId === item.routineId
            ? {
                ...entry,
                completed: !!updatedActivity.completed
              }
            : entry
        )
      );
    } catch (error) {
      setStatus(error.message || "Failed to update routine.");
    } finally {
      setBusyRoutineId(null);
    }
  }

  return (
    <AppSidebarLayout title="Dashboard">
      {status && <p className="status">{status}</p>}

      {loading ? (
        <p className="info-text">Loading dashboard...</p>
      ) : (
        <>
          <h1 className="dashboard-hello">
            Hello{account?.username ? `, ${account.username}` : ""}.
          </h1>

          <div className="dashboard-grid">
            <section className="panel-card">
              <h3 className="panel-title">Tasks</h3>

              <section className="task-section">
                <h4 className="task-section-title">Overdue</h4>
                {overdueTasks.length === 0 ? (
                  <p className="info-text">No overdue tasks.</p>
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
                        {overdueTasks.map((task) => (
                          <tr key={task.id}>
                            <td>{task.description}</td>
                            <td>{formatDate(task.dueDate)}</td>
                            <td className="task-actions-cell">
                              <button
                                type="button"
                                className="table-action"
                                disabled={busyTaskId === task.id}
                                onClick={() => handleSetTaskCompleted(task, true)}
                              >
                                Done
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
                <h4 className="task-section-title">Upcoming Tasks</h4>
                {upcomingTasks.length === 0 ? (
                  <p className="info-text">No upcoming tasks.</p>
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
                        {upcomingTasks.map((task) => (
                          <tr key={task.id}>
                            <td>{task.description}</td>
                            <td>{formatDate(task.dueDate)}</td>
                            <td className="task-actions-cell">
                              <button
                                type="button"
                                className="table-action"
                                disabled={busyTaskId === task.id}
                                onClick={() => handleSetTaskCompleted(task, true)}
                              >
                                Done
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </section>

            <section className="panel-card">
              <h3 className="panel-title">Today's Routines</h3>
              {todayRoutineItems.length === 0 ? (
                <p className="info-text">No routines scheduled for today.</p>
              ) : (
                <ul className="dashboard-routine-list">
                  {todayRoutineItems.map((item) => (
                    <li key={item.routineId} className={item.completed ? "dashboard-routine-item done" : "dashboard-routine-item open"}>
                      <span>{item.routineName}</span>
                      <div className="dashboard-routine-actions">
                        <strong>{item.completed ? "Completed" : "Open"}</strong>
                        {!item.completed && (
                          <button
                            type="button"
                            className="table-action"
                            disabled={busyRoutineId === item.routineId}
                            onClick={() => handleSetTodayRoutineCompleted(item, true)}
                          >
                            Done
                          </button>
                        )}
                        <button
                          type="button"
                          className="table-action"
                          onClick={() => navigate(`/routines?routineId=${encodeURIComponent(String(item.routineId))}`)}
                        >
                          View
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      )}
    </AppSidebarLayout>
  );
}

function getTodayDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
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