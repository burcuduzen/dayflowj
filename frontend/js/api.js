async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`/api/tasks${path}`, {
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
  list: () => request(''),
  create: data => request('', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  status: (id, status) => request(`/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  remove: id => request(`/${id}`, { method: 'DELETE' })
};
