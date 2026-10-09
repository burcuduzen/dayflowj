export async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(path, {
      ...options,
      headers: options.body ? { 'Content-Type': 'application/json' } : undefined
    });
  } catch {
    throw new Error('Backend’e ulaşılamadı. Uygulamanın çalıştığını kontrol edip tekrar dene.');
  }
  if (!response.ok) {
    let problem;
    try { problem = await response.json(); } catch { /* response may not be JSON */ }
    const details = problem?.errors ? Object.values(problem.errors).join(' · ') : problem?.detail;
    throw new Error(details || `İşlem tamamlanamadı (${response.status}).`);
  }
  return response.status === 204 ? null : response.json();
}
export const taskApi = {
  list: () => request('/api/tasks'),
  create: data => request('/api/tasks', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/api/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  status: (id, status) => request(`/api/tasks/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  remove: id => request(`/api/tasks/${id}`, { method: 'DELETE' })
};

export const noteApi = {
  list: () => request('/api/notes'),
  create: data => request('/api/notes', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/api/notes/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  remove: id => request(`/api/notes/${id}`, { method: 'DELETE' })
};
export const reminderApi = {
  list: () => request('/api/reminders'),
  dismiss: id => request(`/api/reminders/${id}/dismiss`, { method: 'POST' }),
  snooze: (id, minutes) => request(`/api/reminders/${id}/snooze`, { method: 'POST', body: JSON.stringify({ minutes }) })
};
