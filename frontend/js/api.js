export async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(path, {
      ...options,
      credentials: 'same-origin',
      headers: { 'X-DayFlowJ-Request': '1', ...(options.body ? {'Content-Type':'application/json'} : {}) }
    });
  } catch {
    throw new Error('Backend’e ulaşılamadı. Uygulamanın çalıştığını kontrol edip tekrar dene.');
  }
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/api/auth/')) window.dispatchEvent(new Event('dayflow-auth-required'));
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

export const progressApi = { get: zone => request(`/api/progress?timeZone=${encodeURIComponent(zone)}`) };
export const naturalTaskApi = {
  preview: data => request('/api/tasks/natural-language/preview', {method:'POST',body:JSON.stringify(data)}),
  create: data => request('/api/tasks/natural-language', {method:'POST',body:JSON.stringify(data)})
};
export const settingsApi = {
  get: () => request('/api/settings'),
  save: data => request('/api/settings', {method:'PUT',body:JSON.stringify(data)})
};
export const focusApi = {
  list: () => request('/api/focus-sessions'),
  active: () => request('/api/focus-sessions/active'),
  start: taskId => request('/api/focus-sessions', {method:'POST',body:JSON.stringify(taskId ? {taskId} : {})}),
  pause: id => request(`/api/focus-sessions/${id}/pause`, {method:'POST'}),
  resume: id => request(`/api/focus-sessions/${id}/resume`, {method:'POST'}),
  complete: id => request(`/api/focus-sessions/${id}/complete`, {method:'POST'})
};
export const analyticsApi = { get: zone => request(`/api/analytics?timeZone=${encodeURIComponent(zone)}`) };
export const planningApi = {
  suggest: data => request('/api/planning/suggestions', {method:'POST',body:JSON.stringify(data)}),
  blocks: () => request('/api/planning/blocks'),
  approve: blocks => request('/api/planning/blocks/approve', {method:'POST',body:JSON.stringify({blocks})}),
  remove: id => request(`/api/planning/blocks/${id}`, {method:'DELETE'})
};
export const calendarEventApi = {
  list: () => request('/api/calendar-events'),
  create: data => request('/api/calendar-events', {method:'POST',body:JSON.stringify(data)}),
  update: (id,data) => request(`/api/calendar-events/${id}`, {method:'PUT',body:JSON.stringify(data)}),
  remove: id => request(`/api/calendar-events/${id}`, {method:'DELETE'})
};
export const emailApi = {
  get: () => request('/api/email/settings'),
  save: data => request('/api/email/settings', {method:'PUT',body:JSON.stringify(data)}),
  test: () => request('/api/email/test', {method:'POST'})
};

export const authApi = {
  status: () => request('/api/auth/status'),
  register: data => request('/api/auth/register', {method:'POST',body:JSON.stringify(data)}),
  login: data => request('/api/auth/login', {method:'POST',body:JSON.stringify(data)}),
  resend: data => request('/api/auth/resend', {method:'POST',body:JSON.stringify(data)}),
  logout: () => request('/api/auth/logout', {method:'POST'})
};
