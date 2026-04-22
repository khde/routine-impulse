import { useEffect, useMemo, useState } from "react";
import AppSidebarLayout from "../components/AppSidebarLayout";
import { parseError } from "../utils/parseError";

const ROUTINE_API_BASE = "/api/routine";
const DAY_OPTIONS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY"
];

export default function RoutinesPage({ apiFetch }) {
  const [routines, setRoutines] = useState([]);
  const [selectedRoutineId, setSelectedRoutineId] = useState(null);
  const [rangeActivity, setRangeActivity] = useState([]);
  const [weekActivity, setWeekActivity] = useState([]);
  const [loadingRoutines, setLoadingRoutines] = useState(true);
  const [loadingRangeActivity, setLoadingRangeActivity] = useState(false);
  const [loadingWeekActivity, setLoadingWeekActivity] = useState(false);
  const [status, setStatus] = useState("");
  const [showRoutineModal, setShowRoutineModal] = useState(false);
  const [editingRoutineId, setEditingRoutineId] = useState(null);
  const [routineName, setRoutineName] = useState("");
  const [routineDescription, setRoutineDescription] = useState("");
  const [selectedDays, setSelectedDays] = useState([]);
  const [savingRoutine, setSavingRoutine] = useState(false);
  const [busyRoutineId, setBusyRoutineId] = useState(null);
  const [busyActivityDate, setBusyActivityDate] = useState("");
  const [activityFrom, setActivityFrom] = useState(getDaysAgoDateString(120));
  const [activityTo, setActivityTo] = useState(getTodayDateString());

  const selectedRoutine = useMemo(
    () => routines.find((routine) => routine.id === selectedRoutineId) || null,
    [routines, selectedRoutineId]
  );

  const weekItems = useMemo(
    () => buildWeekItems(selectedRoutine, weekActivity),
    [selectedRoutine, weekActivity]
  );

  const heatmapItems = useMemo(
    () => buildHeatmapItems(activityFrom, activityTo, selectedRoutine, rangeActivity),
    [activityFrom, activityTo, selectedRoutine, rangeActivity]
  );

  useEffect(() => {
    loadRoutines();
  }, []);

  useEffect(() => {
    if (!selectedRoutineId) {
      setRangeActivity([]);
      return;
    }
    loadRangeActivity(selectedRoutineId);
  }, [selectedRoutineId, activityFrom, activityTo]);

  useEffect(() => {
    if (!selectedRoutineId) {
      setWeekActivity([]);
      return;
    }
    loadWeekActivity(selectedRoutineId);
  }, [selectedRoutineId]);

  async function loadRoutines() {
    setLoadingRoutines(true);
    setStatus("");

    try {
      const response = await apiFetch(`${ROUTINE_API_BASE}/all`, { method: "GET" });
      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const data = await response.json();
      const sorted = Array.isArray(data)
        ? [...data].sort((left, right) => (right.id || 0) - (left.id || 0))
        : [];

      setRoutines(sorted);

      if (sorted.length === 0) {
        setSelectedRoutineId(null);
      } else if (!sorted.some((routine) => routine.id === selectedRoutineId)) {
        setSelectedRoutineId(sorted[0].id);
      }
    } catch (error) {
      setStatus(error.message || "Failed to load routines.");
    } finally {
      setLoadingRoutines(false);
    }
  }

  async function loadRangeActivity(routineId) {
    if (!activityFrom || !activityTo) {
      setRangeActivity([]);
      return;
    }

    setLoadingRangeActivity(true);

    try {
      const query = new URLSearchParams({
        from: activityFrom,
        to: activityTo,
        status: "ALL"
      });

      const response = await apiFetch(`${ROUTINE_API_BASE}/${routineId}/activity?${query.toString()}`, {
        method: "GET"
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const data = await response.json();
      setRangeActivity(Array.isArray(data) ? data : []);
    } catch (error) {
      setRangeActivity([]);
      setStatus(error.message || "Failed to load routine activity.");
    } finally {
      setLoadingRangeActivity(false);
    }
  }

  async function loadWeekActivity(routineId) {
    const weekStart = getWeekStartDateString();
    const weekEnd = getWeekEndDateString();

    setLoadingWeekActivity(true);

    try {
      const query = new URLSearchParams({
        from: weekStart,
        to: weekEnd,
        status: "ALL"
      });

      const response = await apiFetch(`${ROUTINE_API_BASE}/${routineId}/activity?${query.toString()}`, {
        method: "GET"
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const data = await response.json();
      setWeekActivity(Array.isArray(data) ? data : []);
    } catch (error) {
      setWeekActivity([]);
      setStatus(error.message || "Failed to load weekly activity.");
    } finally {
      setLoadingWeekActivity(false);
    }
  }

  function openCreateRoutineModal() {
    setEditingRoutineId(null);
    setRoutineName("");
    setRoutineDescription("");
    setSelectedDays([]);
    setShowRoutineModal(true);
    setStatus("");
  }

  function openEditRoutineModal(routine) {
    setEditingRoutineId(routine.id);
    setRoutineName(routine.name || "");
    setRoutineDescription(routine.description || "");
    setSelectedDays(Array.isArray(routine.selectedDays) ? routine.selectedDays : []);
    setShowRoutineModal(true);
    setStatus("");
  }

  function toggleDay(day) {
    setSelectedDays((prev) =>
      prev.includes(day) ? prev.filter((value) => value !== day) : [...prev, day]
    );
  }

  async function handleSaveRoutine(event) {
    event.preventDefault();

    const trimmedName = routineName.trim();
    if (!trimmedName) {
      setStatus("Routine name is required.");
      return;
    }

    if (selectedDays.length === 0) {
      setStatus("Select at least one day for the routine.");
      return;
    }

    setSavingRoutine(true);
    setStatus("");

    try {
      const payload = {
        name: trimmedName,
        description: routineDescription.trim(),
        selectedDays
      };

      if (!payload.description) {
        payload.description = null;
      }

      const isEdit = editingRoutineId !== null;
      const path = isEdit ? `${ROUTINE_API_BASE}/${editingRoutineId}` : ROUTINE_API_BASE;
      const method = isEdit ? "PATCH" : "POST";

      const response = await apiFetch(path, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const savedRoutine = await response.json();

      if (isEdit) {
        setRoutines((prev) => prev.map((routine) => (routine.id === savedRoutine.id ? savedRoutine : routine)));
        setStatus("Routine updated.");
      } else {
        setRoutines((prev) => [savedRoutine, ...prev]);
        setSelectedRoutineId(savedRoutine.id);
        setStatus("Routine created.");
      }

      setShowRoutineModal(false);
    } catch (error) {
      setStatus(error.message || "Failed to save routine.");
    } finally {
      setSavingRoutine(false);
    }
  }

  async function handleDeleteRoutine(routineId) {
    if (!window.confirm("Delete this routine?")) {
      return;
    }

    setBusyRoutineId(routineId);
    setStatus("");

    try {
      const response = await apiFetch(`${ROUTINE_API_BASE}/${routineId}`, { method: "DELETE" });
      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const remaining = routines.filter((routine) => routine.id !== routineId);
      setRoutines(remaining);

      if (selectedRoutineId === routineId) {
        setSelectedRoutineId(remaining[0]?.id || null);
      }

      setStatus("Routine deleted.");
    } catch (error) {
      setStatus(error.message || "Failed to delete routine.");
    } finally {
      setBusyRoutineId(null);
    }
  }

  async function handleSetActivityCompleted(item, completed) {
    if (!selectedRoutineId) {
      return;
    }

    setBusyActivityDate(item.date);
    setStatus("");

    try {
      const response = await apiFetch(`${ROUTINE_API_BASE}/${selectedRoutineId}/activity`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          date: item.date,
          completed
        })
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const updated = await response.json();

      setWeekActivity((prev) => {
        const next = prev.filter((entry) => entry.date !== updated.date);
        return [...next, updated];
      });

      setRangeActivity((prev) => {
        const next = prev.filter((entry) => entry.date !== updated.date);
        return [...next, updated];
      });

      await Promise.all([
        loadWeekActivity(selectedRoutineId),
        loadRangeActivity(selectedRoutineId)
      ]);
    } catch (error) {
      setStatus(error.message || "Failed to update activity.");
    } finally {
      setBusyActivityDate("");
    }
  }

  return (
    <AppSidebarLayout
      title="Routines"
      subtitle="Create routines, define schedule days, and manage each routine's activity slot."
    >
      <div className="tasks-toolbar">
        <button type="button" className="tasks-new-button" onClick={openCreateRoutineModal}>
          New Routine
        </button>
      </div>

      {status && <p className="status">{status}</p>}

      <div className="routines-grid">
        <section className="panel-card">
          <h3 className="panel-title">All Routines</h3>
          {loadingRoutines ? (
            <p className="info-text">Loading routines...</p>
          ) : routines.length === 0 ? (
            <p className="info-text">No routines yet.</p>
          ) : (
            <div className="routine-list">
              {routines.map((routine) => (
                <article
                  key={routine.id}
                  className={selectedRoutineId === routine.id ? "routine-list-item active" : "routine-list-item"}
                >
                  <button
                    type="button"
                    className="routine-select"
                    onClick={() => setSelectedRoutineId(routine.id)}
                  >
                    <strong className="routine-name">{routine.name}</strong>
                    <span className="routine-description">{routine.description || "No description"}</span>
                    <span className="day-pills">
                      {Array.isArray(routine.selectedDays) && routine.selectedDays.length > 0
                        ? routine.selectedDays.map((day) => (
                            <span key={day} className="day-pill">{day.slice(0, 3)}</span>
                          ))
                        : <span className="day-pill">No days</span>}
                    </span>
                  </button>

                  <div className="routine-item-actions">
                    <button type="button" className="table-action" onClick={() => openEditRoutineModal(routine)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="table-action danger"
                      disabled={busyRoutineId === routine.id}
                      onClick={() => handleDeleteRoutine(routine.id)}
                    >
                      Delete
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="panel-card">
          <h3 className="panel-title routine-main-title">{selectedRoutine ? selectedRoutine.name : "Routine"}</h3>
          {!selectedRoutine ? (
            <p className="info-text">Select a routine to view activity.</p>
          ) : (
            <>
              <h4 className="activity-section-title">Weekly Activity</h4>
              {loadingWeekActivity ? (
                <p className="info-text">Loading week...</p>
              ) : (
                <div className="week-grid">
                  {weekItems.map((item) => (
                    <article
                      key={item.date}
                      className={
                        item.scheduled
                          ? item.completed
                            ? "week-day-card done"
                            : "week-day-card open"
                          : "week-day-card off"
                      }
                    >
                      <p className="week-day-name">{item.label}</p>
                      <p className="week-day-date">{item.date}</p>
                      <p className="week-day-status">
                        {!item.scheduled
                          ? "Not scheduled"
                          : item.completed
                            ? "Completed"
                            : item.isFuture
                              ? "Upcoming"
                              : "Open"}
                      </p>
                      {item.scheduled && !item.isFuture && (
                        <button
                          type="button"
                          className="table-action"
                          disabled={busyActivityDate === item.date}
                          onClick={() => handleSetActivityCompleted(item, !item.completed)}
                        >
                          {item.completed ? "Mark open" : "Mark done"}
                        </button>
                      )}
                    </article>
                  ))}
                </div>
              )}

              <h4 className="activity-section-title heatmap-title">Activity Heatmap</h4>

              <div className="activity-toolbar">
                <label>
                  From
                  <input
                    type="date"
                    value={activityFrom}
                    onChange={(event) => setActivityFrom(event.target.value)}
                  />
                </label>
                <label>
                  To
                  <input
                    type="date"
                    value={activityTo}
                    onChange={(event) => setActivityTo(event.target.value)}
                  />
                </label>
              </div>

              {loadingRangeActivity ? (
                <p className="info-text">Loading activity...</p>
              ) : (
                <>
                  <div className="heatmap-wrap" role="img" aria-label="Routine activity heatmap">
                    {heatmapItems.map((item) => (
                      <span
                        key={item.date}
                        className={`heatmap-cell ${item.level}`}
                        title={`${item.date} - ${item.tooltip}`}
                      />
                    ))}
                  </div>
                  <div className="heatmap-legend">
                    <span><em className="heatmap-cell none" /> Not scheduled</span>
                    <span><em className="heatmap-cell open" /> Scheduled, open</span>
                    <span><em className="heatmap-cell done" /> Scheduled, done</span>
                  </div>
                </>
              )}

              {!loadingRangeActivity && heatmapItems.length === 0 && (
                <p className="info-text">No days in selected range.</p>
              )}

              {heatmapItems.length > 0 && (
                <div className="heatmap-actions">
                  <button
                    type="button"
                    className="table-action"
                    onClick={() => {
                      setActivityFrom(getDaysAgoDateString(90));
                      setActivityTo(getTodayDateString());
                    }}
                  >
                    Last 90 days
                  </button>
                  <button
                    type="button"
                    className="table-action"
                    onClick={() => {
                      setActivityFrom(getDaysAgoDateString(180));
                      setActivityTo(getTodayDateString());
                    }}
                  >
                    Last 180 days
                  </button>
                </div>
              )}
            </>
          )}
        </section>
      </div>

      {showRoutineModal && (
        <div className="modal-overlay" role="presentation" onClick={() => !savingRoutine && setShowRoutineModal(false)}>
          <section
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-label={editingRoutineId ? "Edit routine" : "Create routine"}
            onClick={(event) => event.stopPropagation()}
          >
            <h3 className="modal-title">{editingRoutineId ? "Edit Routine" : "Create New Routine"}</h3>
            <form className="task-create-form" onSubmit={handleSaveRoutine}>
              <label>
                Name
                <input
                  type="text"
                  value={routineName}
                  onChange={(event) => setRoutineName(event.target.value)}
                  maxLength={255}
                  required
                  autoFocus
                  disabled={savingRoutine}
                />
              </label>

              <label>
                Description (optional)
                <textarea
                  className="routine-description-input"
                  value={routineDescription}
                  onChange={(event) => setRoutineDescription(event.target.value)}
                  maxLength={255}
                  disabled={savingRoutine}
                />
              </label>

              <fieldset className="weekday-fieldset" disabled={savingRoutine}>
                <legend>Scheduled Days</legend>
                <div className="weekday-grid">
                  {DAY_OPTIONS.map((day) => (
                    <label key={day} className="weekday-option">
                      <input
                        type="checkbox"
                        checked={selectedDays.includes(day)}
                        onChange={() => toggleDay(day)}
                      />
                      <span>{day.slice(0, 3)}</span>
                    </label>
                  ))}
                </div>
              </fieldset>

              <div className="modal-actions">
                <button type="submit" className="primary" disabled={savingRoutine}>
                  {savingRoutine ? "Saving..." : editingRoutineId ? "Update Routine" : "Save Routine"}
                </button>
                <button
                  type="button"
                  className="secondary"
                  disabled={savingRoutine}
                  onClick={() => setShowRoutineModal(false)}
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

function getTodayDateString() {
  return toLocalDateString(new Date());
}

function getDaysAgoDateString(days) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return toLocalDateString(date);
}

function getCurrentMonthStart() {
  const date = new Date();
  date.setDate(1);
  return toLocalDateString(date);
}

function getWeekStartDateString() {
  const date = new Date();
  const day = date.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  date.setDate(date.getDate() + diff);
  return toLocalDateString(date);
}

function getWeekEndDateString() {
  const date = new Date(getWeekStartDateString());
  date.setDate(date.getDate() + 6);
  return toLocalDateString(date);
}

function buildWeekItems(selectedRoutine, weekActivity) {
  if (!selectedRoutine) {
    return [];
  }

  const activityByDate = new Map((weekActivity || []).map((entry) => [entry.date, entry]));
  const scheduledDays = new Set(selectedRoutine.selectedDays || []);
  const start = new Date(getWeekStartDateString());
  const today = getTodayDateString();
  const labels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const items = [];

  for (let index = 0; index < 7; index += 1) {
    const current = new Date(start);
    current.setDate(start.getDate() + index);
    const dateString = toLocalDateString(current);
    const dayKey = DAY_OPTIONS[index];
    const scheduled = scheduledDays.has(dayKey);
    const item = activityByDate.get(dateString);

    items.push({
      label: labels[index],
      date: dateString,
      scheduled,
      completed: !!item?.completed,
      isFuture: dateString > today
    });
  }

  return items;
}

function buildHeatmapItems(from, to, selectedRoutine, rangeActivity) {
  if (!selectedRoutine || !from || !to || from > to) {
    return [];
  }

  const scheduledDays = new Set(selectedRoutine.selectedDays || []);
  const activityByDate = new Map((rangeActivity || []).map((entry) => [entry.date, entry]));
  const result = [];

  const start = new Date(from);
  const end = new Date(to);

  for (let cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) {
    const dateString = toLocalDateString(cursor);
    const dayIndex = cursor.getDay() === 0 ? 6 : cursor.getDay() - 1;
    const dayKey = DAY_OPTIONS[dayIndex];
    const scheduled = scheduledDays.has(dayKey);
    const activity = activityByDate.get(dateString);
    const completed = !!activity?.completed;

    if (!scheduled) {
      result.push({ date: dateString, level: "none", tooltip: "Not scheduled" });
    } else if (completed) {
      result.push({ date: dateString, level: "done", tooltip: "Scheduled and completed" });
    } else {
      result.push({ date: dateString, level: "open", tooltip: "Scheduled and open" });
    }
  }

  return result;
}

function toLocalDateString(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}