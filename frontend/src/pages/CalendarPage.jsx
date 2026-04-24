import { useEffect, useMemo, useState } from "react";
import AppSidebarLayout from "../components/AppSidebarLayout";
import { parseError } from "../utils/parseError";

const TASK_API_BASE = "/api/task";
const ROUTINE_API_BASE = "/api/routine";

export default function CalendarPage({ apiFetch }) {
  const [tasks, setTasks] = useState([]);
  const [routines, setRoutines] = useState([]);
  const [routineActivities, setRoutineActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState("");
  const [focusDate, setFocusDate] = useState(getTodayDateString());
  const [selectedDate, setSelectedDate] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);

  const monthRange = useMemo(() => getMonthRange(focusDate), [focusDate]);

  const calendarItems = useMemo(() => {
    const itemsByDate = {};

    // Add tasks with due dates
    tasks.forEach((task) => {
      if (task.dueDate) {
        const dateKey = extractDateFromDateTime(task.dueDate);
        if (!itemsByDate[dateKey]) {
          itemsByDate[dateKey] = [];
        }
        itemsByDate[dateKey].push({
          id: task.id,
          type: "task",
          name: task.description,
          completed: task.completed,
          dueDate: task.dueDate
        });
      }
    });

    // Add routine activities
    routineActivities.forEach((activity) => {
      const dateKey = activity.date;
      if (!itemsByDate[dateKey]) {
        itemsByDate[dateKey] = [];
      }
      itemsByDate[dateKey].push({
        id: `routine-${activity.routineId}-${activity.date}`,
        type: "routine",
        name: activity.routineName || "Routine",
        completed: activity.completed,
        date: activity.date
      });
    });

    return itemsByDate;
  }, [tasks, routineActivities]);

  const calendarLayout = useMemo(() => buildCalendarLayout(monthRange.start, monthRange.end, calendarItems), [
    monthRange.start,
    monthRange.end,
    calendarItems
  ]);

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    loadRoutineActivitiesForMonth();
  }, [focusDate, routines]);

  async function loadData() {
    setLoading(true);
    setStatus("");

    try {
      const [tasksRes, routinesRes] = await Promise.all([
        apiFetch(`${TASK_API_BASE}/all`, { method: "GET" }),
        apiFetch(`${ROUTINE_API_BASE}/all`, { method: "GET" })
      ]);

      if (!tasksRes.ok) {
        throw new Error(await parseError(tasksRes));
      }
      if (!routinesRes.ok) {
        throw new Error(await parseError(routinesRes));
      }

      const tasksData = await tasksRes.json();
      const routinesData = await routinesRes.json();

      setTasks(Array.isArray(tasksData) ? tasksData : []);
      setRoutines(Array.isArray(routinesData) ? routinesData : []);
    } catch (error) {
      setStatus(error.message || "Failed to load calendar data.");
    } finally {
      setLoading(false);
    }
  }

  async function loadRoutineActivitiesForMonth() {
    if (routines.length === 0) {
      setRoutineActivities([]);
      return;
    }

    try {
      const query = new URLSearchParams({
        from: monthRange.start,
        to: monthRange.end,
        status: "ALL"
      });

      const allActivities = [];

      for (const routine of routines) {
        const routineCreationDate = extractDateFromDateTime(routine.creationDate);
        const response = await apiFetch(
          `${ROUTINE_API_BASE}/${routine.id}/activity?${query.toString()}`,
          { method: "GET" }
        );

        if (!response.ok) {
          continue;
        }

        const data = await response.json();
        if (Array.isArray(data)) {
          allActivities.push(
            ...data
              .filter((activity) => !routineCreationDate || activity.date >= routineCreationDate)
              .map((activity) => ({
                ...activity,
                routineId: routine.id,
                routineName: routine.name
              }))
          );
        }
      }

      setRoutineActivities(allActivities);
    } catch (error) {
      setStatus(error.message || "Failed to load routine activities.");
    }
  }

  function handleDateClick(date) {
    setSelectedDate(date);
    setShowDetailModal(true);
  }

  function handlePrevMonth() {
    setFocusDate((current) => shiftMonthDate(current, -1));
  }

  function handleNextMonth() {
    setFocusDate((current) => shiftMonthDate(current, 1));
  }

  function handleTodayClick() {
    setFocusDate(getTodayDateString());
  }

  const selectedDateItems = selectedDate ? calendarItems[selectedDate] || [] : [];

  return (
    <AppSidebarLayout title="Calendar" subtitle="View all tasks and routines">
      <div className="calendar-toolbar">
        <div className="calendar-controls">
          <button type="button" className="table-action" onClick={handlePrevMonth}>
            ←
          </button>
          <div className="calendar-month-label">{monthRange.label}</div>
          <button type="button" className="table-action" onClick={handleNextMonth}>
            →
          </button>
          <button type="button" className="table-action" onClick={handleTodayClick}>
            Today
          </button>
        </div>
      </div>

      {status && <p className="status">{status}</p>}

      {loading ? (
        <p className="info-text">Loading calendar...</p>
      ) : (
        <div className="calendar-container">
          <div className="calendar-weekdays">
            {["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((day) => (
              <div key={day} className="calendar-weekday-label">
                {day}
              </div>
            ))}
          </div>

          <div className="calendar-grid">
            {calendarLayout.cells.map((item, index) => (
              <div
                key={index}
                className={`calendar-cell ${item ? (item.inMonth ? "in-month" : "out-month") : ""}`}
              >
                {item && item.inMonth && (
                  <>
                    <div className="calendar-cell-date">{item.dayNumber}</div>
                    {item.items && item.items.length > 0 && (
                      <button
                        type="button"
                        className="calendar-cell-content"
                        onClick={() => handleDateClick(item.date)}
                        title={`${item.items.length} item(s) on ${item.date}`}
                      >
                        <ul className="calendar-items-list">
                          {item.items.slice(0, 3).map((calItem, i) => (
                            <li
                              key={i}
                              className={`calendar-item ${calItem.type} ${calItem.completed ? "completed" : ""}`}
                            >
                              <span>{calItem.name}</span>
                            </li>
                          ))}
                          {item.items.length > 3 && <li className="calendar-item more">+{item.items.length - 3}</li>}
                        </ul>
                      </button>
                    )}
                  </>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {showDetailModal && selectedDate && (
        <div className="modal-overlay" role="presentation" onClick={() => setShowDetailModal(false)}>
          <section
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-label={`Details for ${selectedDate}`}
            onClick={(event) => event.stopPropagation()}
          >
            <h3 className="modal-title">
              {new Date(selectedDate + "T00:00:00").toLocaleDateString(undefined, {
                weekday: "long",
                year: "numeric",
                month: "long",
                day: "numeric"
              })}
            </h3>

            {selectedDateItems.length === 0 ? (
              <p className="info-text">No items scheduled for this date.</p>
            ) : (
              <div className="calendar-detail-list">
                {selectedDateItems.map((item, i) => (
                  <div key={i} className={`calendar-detail-item ${item.type} ${item.completed ? "completed" : ""}`}>
                    <span className="calendar-detail-type">{item.type === "task" ? "Task" : "Routine"}</span>
                    <span className={`calendar-detail-name ${item.completed ? "strikethrough" : ""}`}>
                      {item.name}
                    </span>
                    {item.completed && <span className="calendar-detail-status">✓ Completed</span>}
                  </div>
                ))}
              </div>
            )}

            <div className="modal-actions">
              <button type="button" className="secondary" onClick={() => setShowDetailModal(false)}>
                Close
              </button>
            </div>
          </section>
        </div>
      )}
    </AppSidebarLayout>
  );
}

// Date utility functions
function getTodayDateString() {
  return toLocalDateString(new Date());
}

function getMonthRange(focusDate) {
  const date = new Date(focusDate);
  const year = date.getFullYear();
  const month = date.getMonth();
  const start = new Date(year, month, 1);
  const end = new Date(year, month + 1, 0);

  return {
    start: toLocalDateString(start),
    end: toLocalDateString(end),
    label: start.toLocaleDateString(undefined, { month: "long", year: "numeric" })
  };
}

function shiftMonthDate(focusDate, direction) {
  const date = new Date(focusDate);
  date.setMonth(date.getMonth() + direction);
  return toLocalDateString(date);
}

function toLocalDateString(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseLocalDate(dateString) {
  const [year, month, day] = dateString.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function startOfWeek(date) {
  const result = new Date(date);
  const day = result.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  result.setDate(result.getDate() + diff);
  result.setHours(0, 0, 0, 0);
  return result;
}

function endOfWeek(date) {
  const result = new Date(date);
  const day = result.getDay();
  const diff = day === 0 ? 0 : 7 - day;
  result.setDate(result.getDate() + diff);
  result.setHours(0, 0, 0, 0);
  return result;
}

function extractDateFromDateTime(dateTimeString) {
  if (!dateTimeString) return null;
  return dateTimeString.split("T")[0];
}

function buildCalendarLayout(from, to, calendarItems) {
  if (!from || !to || from > to) {
    return {
      weekdayLabels: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
      cells: []
    };
  }

  const start = startOfWeek(parseLocalDate(from));
  const end = endOfWeek(parseLocalDate(to));
  const cells = [];

  for (let cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) {
    const dateString = toLocalDateString(cursor);
    const inRange = dateString >= from && dateString <= to;

    const items = calendarItems[dateString] || [];

    cells.push({
      date: dateString,
      dayNumber: cursor.getDate(),
      items,
      inMonth: inRange
    });
  }

  return { cells };
}