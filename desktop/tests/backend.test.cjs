const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const net = require('node:net');
const { Backend } = require('../backend.cjs');
const root = process.env.DAYFLOW_TEST_ROOT || path.resolve(__dirname, '../..');

test(
  'owned backend: authentication, history, backup, restore, persistence and graceful shutdown',
  { timeout: 180000 },
  async () => {
    const dataDirectory = await fs.mkdtemp(
      path.join(os.tmpdir(), 'dayflow-test-'),
    );
    const backend = new Backend({
      root,
      dataDirectory,
      legacyDirectory: path.join(dataDirectory, 'empty'),
      port: 0,
    });
    const request = (url, method = 'GET', body) =>
      fetch(backend.origin + url, {
        method,
        headers: { ...backend.headers, 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    try {
      await backend.start();
      assert.equal((await fetch(backend.origin + '/api/tasks')).status, 403);
      let response = await request('/api/tasks', 'POST', {
        title: 'Lifecycle test',
        priority: 'MEDIUM',
        estimatedMinutes: 30,
        reminderEnabled: false,
      });
      assert.equal(response.status, 201);
      const task = await response.json();
      assert.equal(
        (
          await request(`/api/tasks/${task.id}/status`, 'PATCH', {
            status: 'COMPLETED',
          })
        ).status,
        200,
      );
      assert.equal(
        (
          await request(`/api/tasks/${task.id}`, 'PUT', {
            ...task,
            status: 'COMPLETED',
            estimatedMinutes: 90,
          })
        ).status,
        200,
      );
      let analysis = await (await request('/api/analytics')).json();
      assert.equal(analysis.estimatedMinutes, 30);
      response = await request('/api/backups/export', 'POST');
      assert.equal(response.status, 200);
      const archive = path.join(dataDirectory, 'export.zip');
      await fs.writeFile(archive, Buffer.from(await response.arrayBuffer()));
      assert.equal(
        (await request(`/api/tasks/${task.id}`, 'DELETE')).status,
        204,
      );
      analysis = await (await request('/api/analytics')).json();
      assert.equal(analysis.estimatedMinutes, 30);
      await backend.restore(archive);
      assert.equal((await (await request('/api/tasks')).json()).length, 1);
      const child = backend.child;
      await backend.stop();
      assert.equal(child.exitCode, 0);
      await backend.start();
      assert.equal((await (await request('/api/tasks')).json()).length, 1);
    } finally {
      await backend.stop();
      await fs.rm(dataDirectory, { recursive: true, force: true });
    }
  },
);

test(
  'port conflict does not adopt or stop another service',
  { timeout: 90000 },
  async () => {
    const server = net.createServer((socket) => socket.end());
    await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
    const dataDirectory = await fs.mkdtemp(
      path.join(os.tmpdir(), 'dayflow-conflict-'),
    );
    const backend = new Backend({
      root,
      dataDirectory,
      legacyDirectory: path.join(dataDirectory, 'empty'),
      port: server.address().port,
    });
    try {
      await assert.rejects(backend.start(), /Backend/);
      assert.equal(server.listening, true);
    } finally {
      await backend.stop();
      await new Promise((resolve) => server.close(resolve));
      await fs.rm(dataDirectory, { recursive: true, force: true });
    }
  },
);
